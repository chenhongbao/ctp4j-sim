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

package com.nabiki.ctp4j.sim;

import com.nabiki.ctp4j._x.OP;
import com.nabiki.ctp4j.jni.flag.*;
import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.ctp4j.md.CThostFtdcMdSpi;
import com.nabiki.ctp4j.trader.CThostFtdcTraderSpi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TradeBook extends CThostFtdcMdSpi {
    class OngoingOrder {
        public final CThostFtdcInputOrderField order;
        public final CThostFtdcTraderSpi spi;
        public final CThostFtdcRspUserLoginField user;
        public final List<CThostFtdcOrderField> rtn
                = new LinkedList<>();

        OngoingOrder(CThostFtdcInputOrderField order, CThostFtdcTraderSpi spi,
                     CThostFtdcRspUserLoginField usr) {
            this.order = order;
            this.spi  =spi;
            this.user = usr;
        }

        CThostFtdcOrderField lastRtn() {
            if (this.rtn.size() < 1)
                throw new IllegalStateException("return order empty");
            return this.rtn.get(this.rtn.size() - 1);
        }
    }

    private final static TradeBook book = new TradeBook();
    private final CThostFtdcRspUserLoginField loginProfile
            = new CThostFtdcRspUserLoginField();
    private final Map<String, OngoingOrder> orderRefs = new ConcurrentHashMap<>();
    private final Map<String, OngoingOrder> orderSys = new ConcurrentHashMap<>();
    private final Map<String, CThostFtdcDepthMarketDataField> currentDepths
            = new ConcurrentHashMap<>();

    TradeBook() {
        TickSource.getTickSource().addSPI(this);
        // Init login.
        this.loginProfile.BrokerID = "9999";
        this.loginProfile.SystemName = "Simulation";
        this.loginProfile.FrontID = 1;
        this.loginProfile.SessionID = 0;
    }

    public CThostFtdcRspUserLoginField getLoginProfile() {
        return this.loginProfile;
    }

    public static TradeBook getTradeSource() {
        return book;
    }

    public int enqueue(CThostFtdcInputOrderField order, int requestID,
                       CThostFtdcTraderSpi spi, CThostFtdcRspUserLoginField usr) {
        submitOrder(order, requestID, spi, usr);
        // Check current market data. Trade if it could.
        var depth = this.currentDepths.get(order.InstrumentID);
        if (depth != null)
            tryTrade(depth);
        return 0;
    }

    public int enqueue(CThostFtdcInputOrderActionField action, int requestID,
                       CThostFtdcTraderSpi spi) {
        OngoingOrder ongoing;
        // Find ongoing order.
        if (action.OrderSysID != null && action.OrderSysID.length() > 0)
            ongoing = this.orderSys.get(action.OrderSysID);
        else
            ongoing = this.orderRefs.get
                    (getRefKey(action.OrderRef, action.FrontID, action.SessionID));
        // Try cancel order.
        if (ongoing == null)
            spi.OnRspOrderAction(action,
                    rsp(TThostFtdcErrorCode.ORDER_NOT_FOUND,
                            TThostFtdcErrorMessage.ORDER_NOT_FOUND),
                    requestID, true);
        else if (isOrderDone(ongoing))
            spi.OnRspOrderAction(action,
                    rsp(TThostFtdcErrorCode.INSUITABLE_ORDER_STATUS,
                            TThostFtdcErrorMessage.INSUITABLE_ORDER_STATUS),
                    requestID, true);
        else
            // Cancel order and call SPI.
            spi.OnRtnOrder(cancelOrder(ongoing));
        return 0;
    }

    private CThostFtdcOrderField cancelOrder(OngoingOrder ongoing) {
        var rtn = OP.deepCopy(ongoing.lastRtn());
        if (rtn == null)
            throw new IllegalStateException("create rtn order null");
        // Update last rtn order.
        rtn.OrderStatus = TThostFtdcOrderStatusType.CANCELED;
        rtn.StatusMsg += "已撤单";
        rtn.CancelTime = OP.getTime(LocalTime.now(), null);
        ongoing.rtn.add(rtn);
        return OP.deepCopy(rtn);
    }

    private boolean isOrderDone(OngoingOrder ongoing) {
        if (ongoing.rtn.size() < 1)
            throw new IllegalStateException("return order empty");
        var status = ongoing.lastRtn().OrderStatus;
        return status == TThostFtdcOrderStatusType.ALL_TRADED
                || status == TThostFtdcOrderStatusType.CANCELED;
    }

    private CThostFtdcRspInfoField rsp(int errorID, String errorMsg) {
        var rsp = new CThostFtdcRspInfoField();
        rsp.ErrorID = errorID;
        rsp.ErrorMsg = errorMsg;
        return rsp;
    }

    private void submitOrder(CThostFtdcInputOrderField order, int requestID,
                             CThostFtdcTraderSpi spi,
                             CThostFtdcRspUserLoginField usr) {
        // Try submit order.
        if (!checkUnity(order.OrderRef, usr.FrontID, usr.SessionID))
            // Duplicated order ref.
            // Call SPI.
            spi.OnRspOrderInsert(order,
                    rsp(TThostFtdcErrorCode.DUPLICATE_ORDER_REF,
                            TThostFtdcErrorMessage.DUPLICATE_ORDER_REF),
                    requestID, true);
        else
            // Insert order and call SPI.
            spi.OnRtnOrder(insertOrder(order, requestID, spi, usr));
    }

    private CThostFtdcOrderField insertOrder(CThostFtdcInputOrderField order, int requestID,
                             CThostFtdcTraderSpi spi,
                             CThostFtdcRspUserLoginField usr) {
        var rtn = createRtnOrder(order);
        // Other ID.
        rtn.RequestID = requestID;
        rtn.FrontID = usr.FrontID;
        rtn.SessionID = usr.SessionID;
        // Statuses.
        rtn.OrderStatus = TThostFtdcOrderStatusType.NO_TRADE_QUEUEING;
        rtn.OrderSubmitStatus = TThostFtdcOrderSubmitStatusType.ACCEPTED;
        rtn.StatusMsg = "已提交";
        // Create internal order object.
        var ongoing = new OngoingOrder(OP.deepCopy(order), spi, OP.deepCopy(usr));
        ongoing.order.RequestID = requestID;
        ongoing.rtn.add(rtn);
        // Add mapping.
        this.orderRefs.put(
                getRefKey(order.OrderRef, usr.FrontID, usr.SessionID),
                ongoing);
        this.orderSys.put(rtn.OrderSysID, ongoing);
        return OP.deepCopy(rtn);
    }

    private boolean checkUnity(String orderRef, int frontID, int sessionID) {
        var key = getRefKey(orderRef, frontID, sessionID);
        return !this.orderRefs.containsKey(key);
    }

    private String getRefKey(String orderRef, int frontID, int sessionID) {
        return orderRef + "_" + frontID + "_" + sessionID;
    }

    private CThostFtdcOrderField createRtnOrder(CThostFtdcInputOrderField order) {
        var rtn = new CThostFtdcOrderField();
        // Copy info.
        rtn.AccountID = order.AccountID;
        rtn.BrokerID = order.BrokerID;
        rtn.BusinessUnit = order.BusinessUnit;
        rtn.ClientID = order.ClientID;
        rtn.CombHedgeFlag = order.CombHedgeFlag;
        rtn.CombOffsetFlag = order.CombOffsetFlag;
        rtn.ContingentCondition = order.ContingentCondition;
        rtn.CurrencyID = order.CurrencyID;
        rtn.Direction = order.Direction;
        rtn.ExchangeID = order.ExchangeID;
        rtn.ForceCloseReason = order.ForceCloseReason;
        rtn.GTDDate = order.GTDDate;
        rtn.InstrumentID = order.InstrumentID;
        rtn.InvestorID = order.InvestorID;
        rtn.InvestUnitID = order.InvestUnitID;
        rtn.IPAddress = order.IPAddress;
        rtn.IsAutoSuspend = order.IsAutoSuspend;
        rtn.IsSwapOrder = order.IsSwapOrder;
        rtn.LimitPrice = order.LimitPrice;
        rtn.MacAddress = order.MacAddress;
        rtn.MinVolume = order.MinVolume;
        rtn.OrderPriceType = order.OrderPriceType;
        rtn.OrderRef = order.OrderRef;
        rtn.RequestID = order.RequestID;
        rtn.StopPrice = order.StopPrice;
        rtn.TimeCondition = order.TimeCondition;
        rtn.UserForceClose = order.UserForceClose;
        rtn.UserID = order.UserID;
        rtn.VolumeCondition = order.VolumeCondition;
        rtn.VolumeTotalOriginal = order.VolumeTotalOriginal;
        // Day and time.
        rtn.TradingDay = OP.getTradingDay(LocalDate.now());
        rtn.InsertDate = OP.getDay(LocalDate.now(), null);
        rtn.InsertTime = OP.getTime(LocalTime.now(), null);
        rtn.ActiveTime = rtn.InsertTime;
        // OrderSysID.
        rtn.OrderSysID = getOrderSysID();
        // Volume.
        rtn.VolumeTraded = 0;
        rtn.VolumeTotal = rtn.VolumeTotalOriginal;
        return rtn;
    }

    private String getOrderSysID() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS"));
    }

    @Override
    public void OnRtnDepthMarketData(
            CThostFtdcDepthMarketDataField depthMarketData) {
        // Save latest depth market data.
        this.currentDepths.put(depthMarketData.InstrumentID, depthMarketData);
        // Check ongoing order and trade.
        tryTrade(depthMarketData);
    }

    private void tryTrade(CThostFtdcDepthMarketDataField depthMarketData) {
        for (var ongoing : this.orderSys.values()) {
            if (ongoing.order.InstrumentID
                    .compareToIgnoreCase(depthMarketData.InstrumentID) != 0
                    || isOrderDone(ongoing))
                continue;
            var last = ongoing.lastRtn();
            // Calculate traded volume this time.
            if (last.VolumeTotal <= 0)
                throw new IllegalStateException("volume traded overflow");
            int volumeNow = Math.min(last.VolumeTotal, 11);
            // Check price.
            if (ongoing.order.Direction == TThostFtdcDirectionType.DIRECTION_BUY) {
                if (ongoing.order.LimitPrice >= depthMarketData.AskPrice1)
                    trade(ongoing, depthMarketData.AskPrice1, volumeNow);
            } else {
                if (ongoing.order.LimitPrice <= depthMarketData.BidPrice1)
                    trade(ongoing, depthMarketData.BidPrice1, volumeNow);
            }
        }
    }

    private void trade(OngoingOrder ongoing, double price, int volume) {
        // Call SPI.
        ongoing.spi.OnRtnTrade(createTrade(ongoing, price, volume));
        // Update order and call SPI.
        ongoing.spi.OnRtnOrder(updateOrder(ongoing, volume));
    }

    private CThostFtdcOrderField updateOrder(OngoingOrder ongoing, int volume) {
        var rtn = OP.deepCopy(ongoing.lastRtn());
        if (rtn == null)
            throw new IllegalStateException("create rtn order null");
        rtn.VolumeTraded += volume;
        rtn.VolumeTotal -= volume;
        if (rtn.VolumeTotal > 0) {
            rtn.OrderStatus = TThostFtdcOrderStatusType.PART_TRADED_QUEUEING;
            rtn.StatusMsg += "部分成交";
        } else {
            rtn.OrderStatus = TThostFtdcOrderStatusType.ALL_TRADED;
            rtn.StatusMsg += "全部成交";
        }
        rtn.UpdateTime = OP.getTime(LocalTime.now(), null);
        ongoing.rtn.add(rtn);
        return OP.deepCopy(rtn);
    }

    private CThostFtdcTradeField createTrade(OngoingOrder ongoing, double price,
                                             int volume) {
        var trade = new CThostFtdcTradeField();
        trade.BrokerID = ongoing.order.BrokerID;
        trade.InvestorID = ongoing.order.InvestorID;
        trade.InstrumentID = ongoing.order.InstrumentID;
        trade.OrderRef = ongoing.order.OrderRef;
        trade.UserID = ongoing.order.UserID;
        trade.ExchangeID = ongoing.order.ExchangeID;
        trade.TradeID = String.valueOf(System.nanoTime());
        trade.Direction = ongoing.order.Direction;
        trade.OrderSysID = ongoing.lastRtn().OrderSysID;
        trade.ParticipantID = "";
        trade.ClientID = "";
        trade.TradingRole = 0;
        trade.ExchangeInstID = ongoing.order.InstrumentID;
        trade.OffsetFlag = ongoing.order.CombOffsetFlag;
        trade.HedgeFlag = ongoing.order.CombHedgeFlag;
        trade.Price = price;
        trade.Volume = volume;
        trade.TradeDate = OP.getDay(LocalDate.now(), null);
        trade.TradeTime = OP.getTime(LocalTime.now(), null);
        trade.TradeType = 0;
        trade.PriceSource = 0;
        trade.TraderID = "";
        trade.OrderLocalID = "";
        trade.ClearingPartID = "";
        trade.BusinessUnit = "";
        trade.SequenceNo = 0;
        trade.TradingDay = OP.getTradingDay(LocalDate.now());
        trade.SettlementID = 0;
        trade.BrokerOrderSeq = 0;
        trade.TradeSource = 0;
        trade.InvestUnitID = "";
        return trade;
    }
}
