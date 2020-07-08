/*
 * Copyright (c) 2020 Hongbao Chen <chenhongbao@outlook.com>
 *
 * Licensed under the  GNU Affero General Public License v3.0 and you may not use
 * this file except in compliance with the  License. You may obtain a copy of the
 * License at
 *
 *                    https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Permission is hereby  granted, free of charge, to any  person obtaining a copy
 * of this software and associated  documentation files (the "Software"), to deal
 * in the Software  without restriction, including without  limitation the rights
 * to  use, copy,  modify, merge,  publish, distribute,  sublicense, and/or  sell
 * copies  of  the Software,  and  to  permit persons  to  whom  the Software  is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE  IS PROVIDED "AS  IS", WITHOUT WARRANTY  OF ANY KIND,  EXPRESS OR
 * IMPLIED,  INCLUDING BUT  NOT  LIMITED TO  THE  WARRANTIES OF  MERCHANTABILITY,
 * FITNESS FOR  A PARTICULAR PURPOSE AND  NONINFRINGEMENT. IN NO EVENT  SHALL THE
 * AUTHORS  OR COPYRIGHT  HOLDERS  BE  LIABLE FOR  ANY  CLAIM,  DAMAGES OR  OTHER
 * LIABILITY, WHETHER IN AN ACTION OF  CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE  OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.nabiki.ctp4j.trader.internal;

import com.nabiki.ctp4j._x.OP;
import com.nabiki.ctp4j.jni.flag.TThostFtdcErrorCode;
import com.nabiki.ctp4j.jni.flag.TThostFtdcErrorMessage;
import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.ctp4j.sim.CommonData;
import com.nabiki.ctp4j.sim.TradeBook;
import com.nabiki.ctp4j.trader.CThostFtdcTraderApi;
import com.nabiki.ctp4j.trader.CThostFtdcTraderSpi;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

class CThostFtdcTraderApiImpl extends CThostFtdcTraderApi {
    // Default and silent implementation SPI.
    static class DefaultSPI extends CThostFtdcTraderSpi {
    }

    private final static String apiVersion = "sim_1.0";
    private final Path instrDir;
    private final Logger log;

    private CThostFtdcTraderSpi spi = new DefaultSPI();
    private CThostFtdcRspUserLoginField selfLogin;

    public CThostFtdcTraderApiImpl(String flowDir) {
        OP.ensure(flowDir, ".log");
        OP.ensure(flowDir, ".instrument");
        this.instrDir = Path.of(flowDir, ".instrument");
        // Set logger.
        this.log = Logger.getLogger(this.getClass().getCanonicalName());
        try {
            var h = new FileHandler(
                    Path.of(flowDir, ".log", "trader.log").toString());
            h.setFormatter(new SimpleFormatter());
            this.log.addHandler(h);
        } catch (IOException e) {
            throw new IllegalStateException("fail creating logger");
        }
    }

    @Override
    public String GetApiVersion() {
        return apiVersion;
    }

    @Override
    public String GetTradingDay() {
        return OP.getTradingDay(LocalDate.now());
    }

    @Override
    public void Init() {
        // Load settings.
        this.instrDir.toFile().listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                try {
                    if (file.getName().startsWith("instrument.")) {
                        var in = OP.fromJson(
                                OP.readText(file, StandardCharsets.UTF_8),
                                CThostFtdcInstrumentField.class);
                        CommonData.addInstrument(in.InstrumentID, in);
                    } else if (file.getName().startsWith("commission.")) {
                        var comm = OP.fromJson(
                                OP.readText(file, StandardCharsets.UTF_8),
                                CThostFtdcInstrumentCommissionRateField.class);
                        CommonData.addCommission(comm.InstrumentID, comm);
                    } else if (file.getName().startsWith("margin.")) {
                        var margin = OP.fromJson(
                                OP.readText(file, StandardCharsets.UTF_8),
                                CThostFtdcInstrumentMarginRateField.class);
                        CommonData.addMargin(margin.InstrumentID, margin);
                    }
                } catch (IOException e) {
                    log.warning(e.getMessage());
                }
                return false;
            }
        });
    }

    private CThostFtdcRspInfoField rsp(int errorID, String errorMsg) {
        var rsp = new CThostFtdcRspInfoField();
        rsp.ErrorID = errorID;
        rsp.ErrorMsg = errorMsg;
        return rsp;
    }

    @Override
    public void Join() {
    }

    @Override
    public void SubscribePrivateTopic(int type) {
        this.log.info("SubscribePrivateTopic(" + type + ")");
    }

    @Override
    public void SubscribePublicTopic(int type) {
        this.log.info("SubscribePublicTopic(" + type + ")");
    }

    @Override
    public void RegisterFront(String frontAddress) {
        this.log.info("RegisterFront(" + frontAddress + ")");
    }

    @Override
    public void RegisterSpi(CThostFtdcTraderSpi spi) {
        this.spi = spi;
    }

    @Override
    public void Release() {
        this.spi = null;
    }

    @Override
    public int ReqAuthenticate(CThostFtdcReqAuthenticateField reqAuthenticateField,
                               int requestID) {
        var rspAuth = new CThostFtdcRspAuthenticateField();
        rspAuth.BrokerID = reqAuthenticateField.BrokerID;
        rspAuth.UserID = reqAuthenticateField.UserID;
        rspAuth.AppID = reqAuthenticateField.AppID;
        rspAuth.UserProductInfo = reqAuthenticateField.UserProductInfo;
        try {
            this.spi.OnRspAuthenticate(rspAuth,
                    rsp(TThostFtdcErrorCode.NONE, TThostFtdcErrorMessage.NONE),
                    requestID, true);
        } catch (Throwable th) {
            this.log.severe(th.getMessage());
            return (-1);
        }
        return 0;
    }

    @Override
    public int ReqUserLogin(CThostFtdcReqUserLoginField reqUserLoginField,
                            int requestID) {
        this.selfLogin = OP.deepCopy(TradeBook.getTradeSource().getLoginProfile());
        if (this.selfLogin == null)
            throw new IllegalStateException("login rsp null");
        this.selfLogin.CZCETime = this.selfLogin.DCETime
                = this.selfLogin.FFEXTime
                = this.selfLogin.INETime
                = this.selfLogin.SHFETime
                = this.selfLogin.LoginTime
                = OP.getTime(LocalTime.now(), null);
        this.selfLogin.SessionID += 1;
        this.selfLogin.UserID = reqUserLoginField.UserID;
        this.selfLogin.TradingDay = OP.getTradingDay(LocalDate.now());
        try {
            this.spi.OnRspUserLogin(this.selfLogin,
                    rsp(TThostFtdcErrorCode.NONE, TThostFtdcErrorMessage.NONE),
                    requestID, true);
        } catch (Throwable th) {
            this.log.severe(th.getMessage());
            return (-1);
        }
        return 0;
    }

    @Override
    public int ReqUserLogout(CThostFtdcUserLogoutField userLogout, int requestID) {
        try {
            this.spi.OnRspUserLogout(userLogout,
                    rsp(TThostFtdcErrorCode.NONE, TThostFtdcErrorMessage.NONE),
                    requestID, true);
        } catch (Throwable th) {
            this.log.severe(th.getMessage());
            return (-1);
        }
        return 0;
    }

    @Override
    public int ReqSettlementInfoConfirm(
            CThostFtdcSettlementInfoConfirmField settlementInfoConfirm,
            int requestID) {
        try {
            this.spi.OnRspSettlementInfoConfirm(settlementInfoConfirm,
                    rsp(TThostFtdcErrorCode.NONE, TThostFtdcErrorMessage.NONE),
                    requestID, true);
        } catch (Throwable th) {
            this.log.severe(th.getMessage());
            return (-1);
        }
        return 0;
    }

    @Override
    public int ReqOrderInsert(CThostFtdcInputOrderField inputOrder, int requestID) {
        try {
            return TradeBook.getTradeSource().enqueue(inputOrder, requestID,
                    this.spi, this.selfLogin);
        } catch (Throwable th) {
            this.log.severe(th.getMessage());
            return (-1);
        }
    }

    @Override
    public int ReqOrderAction(CThostFtdcInputOrderActionField inputOrderAction,
                              int requestID) {
        try {
            return TradeBook.getTradeSource().enqueue(inputOrderAction, requestID,
                    this.spi);
        } catch (Throwable th) {
            this.log.severe(th.getMessage());
            return (-1);
        }
    }

    @Override
    public int ReqQryInstrument(CThostFtdcQryInstrumentField qryInstrument,
                                int requestID) {
        try {
            var instrument = CommonData.getInstrument(qryInstrument.InstrumentID);
            if (instrument != null)
                this.spi.OnRspQryInstrument(instrument,
                        rsp(TThostFtdcErrorCode.NONE, TThostFtdcErrorMessage.NONE),
                        requestID, true);
            else
                this.spi.OnRspError(
                        rsp(TThostFtdcErrorCode.INSTRUMENT_NOT_FOUND,
                                TThostFtdcErrorMessage.INSTRUMENT_NOT_FOUND),
                        requestID, true);
        } catch (Throwable th) {
            this.log.severe(th.getMessage());
            return (-1);
        }
        return 0;
    }

    @Override
    public int ReqQryInstrumentCommissionRate(
            CThostFtdcQryInstrumentCommissionRateField qryInstrumentCommissionRate,
            int requestID) {
        try {
            var commission = CommonData.getCommission(
                    qryInstrumentCommissionRate.InstrumentID);
            if (commission != null)
                this.spi.OnRspQryInstrumentCommissionRate(commission,
                        rsp(TThostFtdcErrorCode.NONE, TThostFtdcErrorMessage.NONE),
                        requestID, true);
            else
                this.spi.OnRspError(
                        rsp(TThostFtdcErrorCode.INSTRUMENT_NOT_FOUND,
                                TThostFtdcErrorMessage.INSTRUMENT_NOT_FOUND),
                        requestID, true);
        } catch (Throwable th) {
            this.log.severe(th.getMessage());
            return (-1);
        }
        return 0;
    }

    @Override
    public int ReqQryInstrumentMarginRate(
            CThostFtdcQryInstrumentMarginRateField qryInstrumentMarginRate,
            int requestID) {
        try {
            var margin = CommonData.getMargin(qryInstrumentMarginRate.InstrumentID);
            if (margin != null)
                this.spi.OnRspQryInstrumentMarginRate(margin,
                        rsp(TThostFtdcErrorCode.NONE, TThostFtdcErrorMessage.NONE),
                        requestID, true);
            else
                this.spi.OnRspError(
                        rsp(TThostFtdcErrorCode.INSTRUMENT_NOT_FOUND,
                                TThostFtdcErrorMessage.INSTRUMENT_NOT_FOUND),
                        requestID, true);
        } catch (Throwable th) {
            this.log.severe(th.getMessage());
            return (-1);
        }
        return 0;
    }

    @Override
    public int ReqQryTradingAccount(
            CThostFtdcQryTradingAccountField qryTradingAccount, int requestID) {
        throw new UnsupportedOperationException("no implementation");
    }

    @Override
    public int ReqQryInvestorPositionDetail(
            CThostFtdcQryInvestorPositionDetailField qryInvestorPositionDetail,
            int requestID) {
        throw new UnsupportedOperationException("no implementation");
    }
}
