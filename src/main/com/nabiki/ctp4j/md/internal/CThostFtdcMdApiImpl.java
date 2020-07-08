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

package com.nabiki.ctp4j.md.internal;

import com.nabiki.ctp4j._x.OP;
import com.nabiki.ctp4j.jni.flag.TThostFtdcErrorCode;
import com.nabiki.ctp4j.jni.flag.TThostFtdcErrorMessage;
import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.ctp4j.md.CThostFtdcMdApi;
import com.nabiki.ctp4j.md.CThostFtdcMdSpi;
import com.nabiki.ctp4j.sim.TickSource;
import com.nabiki.ctp4j.sim.TradeBook;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class CThostFtdcMdApiImpl extends CThostFtdcMdApi {
    class SourceHubSPI extends CThostFtdcMdSpi {
        @Override
        public void OnRtnDepthMarketData(
                CThostFtdcDepthMarketDataField depthMarketData) {
            if (subscribed.contains(depthMarketData.InstrumentID))
                spi.OnRtnDepthMarketData(depthMarketData);
        }
    }

    class DefaultSPI extends CThostFtdcMdSpi {}

    private final static String apiVersion = "sim_1.0";
    private final Path logDir, settleDir;
    private final Logger log;
    private final Map<String, CThostFtdcDepthMarketDataField> depths
            = new ConcurrentHashMap<>();
    private final Set<String> subscribed = new ConcurrentSkipListSet<>();

    private CThostFtdcMdSpi spi = new DefaultSPI();
    private final CThostFtdcMdSpi hubSpi = new SourceHubSPI();

    public CThostFtdcMdApiImpl(String flowDir, boolean isUsingUdp,
                               boolean isMulticast) {
        OP.ensure(flowDir, ".log");
        OP.ensure(flowDir, ".settle");
        this.logDir = Path.of(flowDir, ".log");
        this.settleDir = Path.of(flowDir, ".settle");
        // Set SPI hub.
        TickSource.getTickSource().addSPI(this.hubSpi);
        // Set logger.
        this.log = Logger.getLogger(this.getClass().getCanonicalName());
        try {
            var h = new FileHandler(Path.of(
                    this.logDir.toString(), "md.log").toString());
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
        this.settleDir.toFile().listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                try {
                    if (file.getName().startsWith("depth.")) {
                        var depth = OP.fromJson(
                                OP.readText(file, StandardCharsets.UTF_8),
                                CThostFtdcDepthMarketDataField.class);
                        depths.put(depth.InstrumentID, depth);
                    }
                } catch (IOException e) {
                    log.warning(e.getMessage());
                }
                return false;
            }
        });
    }

    @Override
    public void Join() {
    }

    @Override
    public void RegisterFront(String frontAddress) {
        this.log.info("RegisterFront(" + frontAddress + ")");
    }

    @Override
    public void RegisterSpi(CThostFtdcMdSpi spi) {
        this.spi = spi;
    }

    @Override
    public void Release() {
        TickSource.getTickSource().removeSPI(this.hubSpi);
        this.spi = null;
    }

    @Override
    public int ReqUserLogin(CThostFtdcReqUserLoginField reqUserLoginField, int requestID) {
        var rsp = OP.deepCopy(TradeBook.getTradeSource().getLoginProfile());
        rsp.CZCETime = rsp.DCETime
                = rsp.FFEXTime
                = rsp.INETime
                = rsp.SHFETime
                = rsp.LoginTime
                = OP.getTime(LocalTime.now(), null);
        rsp.SessionID += 1;
        rsp.UserID = reqUserLoginField.UserID;
        rsp.TradingDay = OP.getTradingDay(LocalDate.now());
        this.spi.OnRspUserLogin(rsp,
                rsp(TThostFtdcErrorCode.NONE, TThostFtdcErrorMessage.NONE),
                requestID, true);
        return 0;
    }

    @Override
    public int ReqUserLogout(CThostFtdcUserLogoutField userLogout, int requestID) {
        this.spi.OnRspUserLogout(userLogout,
                rsp(TThostFtdcErrorCode.NONE, TThostFtdcErrorMessage.NONE),
                requestID, true);
        return 0;
    }

    @Override
    public int SubscribeMarketData(String[] instrumentID, int count) {
        if (count < 1)
            return 0;
        for (int i = 0; i < count; ++i) {
            var spec = new CThostFtdcSpecificInstrumentField();
            spec.InstrumentID = instrumentID[i];
            boolean last = false;
            if (i == count - 1)
                last = true;
            if (this.depths.containsKey(instrumentID[i])) {
                this.spi.OnRspSubMarketData(spec,
                        rsp(TThostFtdcErrorCode.NONE, TThostFtdcErrorMessage.NONE),
                        0, last);
                // Add.
                this.subscribed.add(instrumentID[i]);
            } else {
                this.spi.OnRspSubMarketData(spec,
                        rsp(TThostFtdcErrorCode.INSTRUMENT_NOT_FOUND,
                                TThostFtdcErrorMessage.INSTRUMENT_NOT_FOUND),
                        0, last);
            }
        }
        return 0;
    }

    @Override
    public int UnSubscribeMarketData(String[] instrumentID, int count) {
        if (count < 1)
            return 0;
        for (int i = 0; i < count; ++i) {
            var spec = new CThostFtdcSpecificInstrumentField();
            spec.InstrumentID = instrumentID[i];
            boolean last = false;
            if (i == count - 1)
                last = true;
            if (subscribed.contains(instrumentID[i])) {
                this.spi.OnRspUnSubMarketData(spec,
                        rsp(TThostFtdcErrorCode.NONE, TThostFtdcErrorMessage.NONE),
                        0, last);
                // Remove.
                this.subscribed.remove(instrumentID[i]);
            } else {
                this.spi.OnRspUnSubMarketData(spec,
                        rsp(TThostFtdcErrorCode.INSTRUMENT_NOT_FOUND,
                                TThostFtdcErrorMessage.INSTRUMENT_NOT_FOUND),
                        0, last);
            }
        }
        return 0;
    }

    private CThostFtdcRspInfoField rsp(int errorID, String errorMsg) {
        var rsp = new CThostFtdcRspInfoField();
        rsp.ErrorID = errorID;
        rsp.ErrorMsg = errorMsg;
        return rsp;
    }
}
