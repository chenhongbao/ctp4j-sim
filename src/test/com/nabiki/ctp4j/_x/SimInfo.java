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

package com.nabiki.ctp4j._x;

import com.nabiki.ctp4j.jni.flag.TThostFtdcCombHedgeFlagType;
import com.nabiki.ctp4j.jni.flag.TThostFtdcProductClassType;
import com.nabiki.ctp4j.jni.struct.CThostFtdcDepthMarketDataField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcInstrumentCommissionRateField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcInstrumentField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcInstrumentMarginRateField;

public class SimInfo {
    public static CThostFtdcInstrumentField[] instruments
            = new CThostFtdcInstrumentField[3];
    public static CThostFtdcInstrumentCommissionRateField[] commissions
            = new CThostFtdcInstrumentCommissionRateField[3];
    public static CThostFtdcInstrumentMarginRateField[] margins
            = new CThostFtdcInstrumentMarginRateField[3];
    public static CThostFtdcDepthMarketDataField[] depths
            = new CThostFtdcDepthMarketDataField[3];

    /**
     * Limit upper/lower price ratio relative to pre settlement price.
     */
    public static final double LIMIT_PRICE_RATIO = 0.05D;

    static {
        var in = new CThostFtdcInstrumentField();
        in.InstrumentID = "X0001";
        in.ExchangeID = "SZFE";
        in.InstrumentName = "仙人草";
        in.ExchangeInstID = "X0001";
        in.ProductID = "X";
        in.ProductClass = TThostFtdcProductClassType.Futures;
        in.DeliveryYear = 9099;
        in.DeliveryMonth = 12;
        in.MaxMarketOrderVolume = 10000;
        in.MinMarketOrderVolume = 1;
        in.MaxLimitOrderVolume = 10000;
        in.MinLimitOrderVolume = 1;
        in.VolumeMultiple = 10;
        in.PriceTick = 1.0D;
        in.CreateDate = OP.getToday(null);
        in.OpenDate = OP.getToday(null);
        in.ExpireDate = "99991215";
        in.StartDelivDate = "99991216";
        in.EndDelivDate = "99991216";
        in.InstLifePhase = 0;
        in.IsTrading = 1;
        in.PositionType = 0;
        in.PositionDateType = 0;
        in.LongMarginRatio = 0.0D;
        in.ShortMarginRatio = 0.0D;
        in.MaxMarginSideAlgorithm = 0;
        in.UnderlyingInstrID = "X0001";
        in.StrikePrice = 0.0D;
        in.OptionsType = 0;
        in.UnderlyingMultiple = 10;
        in.CombinationType = 0;

        var comm = new CThostFtdcInstrumentCommissionRateField();
        comm.InstrumentID =  "X0001";
        comm.InvestorRange = 0;
        comm.BrokerID = "";
        comm.InvestorID = "";
        comm.OpenRatioByMoney = 0.0D;
        comm.OpenRatioByVolume = 1.5D;
        comm.CloseRatioByMoney = 0.0D;
        comm.CloseRatioByVolume = 0.0D;
        comm.CloseTodayRatioByMoney = 0.0D;
        comm.CloseTodayRatioByVolume = 1.5D;
        comm.ExchangeID = "X0001";
        comm.BizType = 0;
        comm.InvestUnitID = "";

        var margin = new CThostFtdcInstrumentMarginRateField();
        margin.InstrumentID = "X0001";
        margin.InvestorRange = 0;
        margin.BrokerID = "";
        margin.InvestorID = "";
        margin.HedgeFlag = TThostFtdcCombHedgeFlagType.SPECULATION;
        margin.LongMarginRatioByMoney = 0.005D;
        margin.LongMarginRatioByVolume = 0.0D;
        margin.ShortMarginRatioByMoney = 0.05D;
        margin.ShortMarginRatioByVolume = 0.0D;
        margin.IsRelative = 0;
        margin.ExchangeID = "X0001";
        margin.InvestUnitID = "";

        var depth = new CThostFtdcDepthMarketDataField();
        depth.TradingDay = "";
        depth.InstrumentID = "X0001";
        depth.ExchangeID = "X0001";
        depth.ExchangeInstID = "X0001";
        depth.LastPrice = 1340.0D;
        depth.PreSettlementPrice = 1330.0D;
        depth.PreClosePrice = depth.LastPrice;
        depth.PreOpenInterest = 3252;
        depth.OpenPrice = depth.LastPrice;
        depth.HighestPrice = depth.LastPrice;
        depth.LowestPrice = depth.LastPrice;
        depth.Volume = 0;
        depth.Turnover = 0;
        depth.OpenInterest = depth.PreOpenInterest;
        depth.ClosePrice = 0.0D;
        depth.SettlementPrice = 0.0D;
        depth.UpperLimitPrice = 1397.0D;
        depth.LowerLimitPrice = 1264.0D;
        depth.PreDelta = 0;
        depth.CurrDelta = 0;
        depth.UpdateTime = "";
        depth.UpdateMillisec = 0;
        depth.BidPrice1 = 1340.0D;
        depth.BidVolume1 = 1000;
        depth.AskPrice1 = 1341.0D;
        depth.AskVolume1 = 1000;
        depth.AveragePrice = 1340.0D;
        depth.ActionDay = "";

        // [1]. X0001
        instruments[0] = in;
        commissions[0] = comm;
        margins[0] = margin;
        depths[0] = depth;

        in = OP.deepCopy(instruments[0]);
        in.InstrumentID = "X0002";
        in.InstrumentName = "参天木";
        in.ExchangeInstID = "X0002";
        in.VolumeMultiple = 100;
        in.PriceTick = 0.5D;
        in.UnderlyingInstrID = "X0002";
        in.UnderlyingMultiple = 100;

        comm = OP.deepCopy(commissions[0]);
        comm.InstrumentID =  "X0002";
        comm.OpenRatioByMoney = 0.00005D;
        comm.OpenRatioByVolume = 0.0D;
        comm.CloseRatioByMoney = 0.0D;
        comm.CloseRatioByVolume = 0.0D;
        comm.CloseTodayRatioByMoney = 0.00005D;
        comm.CloseTodayRatioByVolume = 0.0D;
        comm.ExchangeID = "X0002";

        margin = OP.deepCopy(margins[0]);
        margin.InstrumentID = "X0002";
        margin.LongMarginRatioByMoney = 0.0D;
        margin.LongMarginRatioByVolume = 1000.0D;
        margin.ShortMarginRatioByMoney = 0.0D;
        margin.ShortMarginRatioByVolume = 1000.0D;
        margin.ExchangeID = "X0002";

        depth = OP.deepCopy(depths[0]);
        depth.InstrumentID = "X0002";
        depth.ExchangeID = "X0002";
        depth.ExchangeInstID = "X0002";
        depth.LastPrice = 2340.0D;
        depth.PreSettlementPrice = 2330.0D;
        depth.PreClosePrice = depth.LastPrice;
        depth.PreOpenInterest = 4151;
        depth.OpenPrice = depth.LastPrice;
        depth.HighestPrice = depth.LastPrice;
        depth.LowestPrice = depth.LastPrice;
        depth.OpenInterest = depth.PreOpenInterest;
        depth.UpperLimitPrice = 2447.0D;
        depth.LowerLimitPrice = 2214.0D;
        depth.BidPrice1 = 2340.0D;
        depth.BidVolume1 = 1000;
        depth.AskPrice1 = 2341.0D;
        depth.AskVolume1 = 1000;
        depth.AveragePrice = 2340.0D;

        // [2]. X0002
        instruments[1] = in;
        commissions[1] = comm;
        margins[1] = margin;
        depths[1] = depth;

        in = OP.deepCopy(instruments[0]);
        in.InstrumentID = "X0003";
        in.InstrumentName = "长生花";
        in.ExchangeInstID = "X0003";
        in.VolumeMultiple = 10;
        in.PriceTick = 2.0D;
        in.UnderlyingInstrID = "X0003";
        in.UnderlyingMultiple = 10;

        comm = OP.deepCopy(commissions[0]);
        comm.InstrumentID =  "X0003";
        comm.OpenRatioByMoney = 0.00005D;
        comm.OpenRatioByVolume = 0.0D;
        comm.CloseRatioByMoney = 0.00005D;
        comm.CloseRatioByVolume = 0.0D;
        comm.CloseTodayRatioByMoney = 0.00005D;
        comm.CloseTodayRatioByVolume = 0.0D;
        comm.ExchangeID = "X0003";

        margin = OP.deepCopy(margins[0]);
        margin.InstrumentID = "X0003";
        margin.LongMarginRatioByMoney = 0.07D;
        margin.LongMarginRatioByVolume = 0.0D;
        margin.ShortMarginRatioByMoney = 0.07D;
        margin.ShortMarginRatioByVolume = 0.0D;
        margin.ExchangeID = "X0003";

        depth = OP.deepCopy(depths[0]);
        depth.InstrumentID = "X0003";
        depth.ExchangeID = "X0003";
        depth.ExchangeInstID = "X0003";
        depth.LastPrice = 3340.0D;
        depth.PreSettlementPrice = 3330.0D;
        depth.PreClosePrice = depth.LastPrice;
        depth.PreOpenInterest = 5353;
        depth.OpenPrice = depth.LastPrice;
        depth.HighestPrice = depth.LastPrice;
        depth.LowestPrice = depth.LastPrice;
        depth.OpenInterest = depth.PreOpenInterest;
        depth.UpperLimitPrice = 3497.0D;
        depth.LowerLimitPrice = 3164.0D;
        depth.BidPrice1 = 3340.0D;
        depth.BidVolume1 = 1000;
        depth.AskPrice1 = 3341.0D;
        depth.AskVolume1 = 1000;
        depth.AveragePrice = 3340.0D;

        // [3]. X0003
        instruments[2] = in;
        commissions[2] = comm;
        margins[2] = margin;
        depths[2] = depth;
    }
}
