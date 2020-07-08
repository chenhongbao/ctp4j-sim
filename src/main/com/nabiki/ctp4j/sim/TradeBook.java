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

import com.nabiki.ctp4j.jni.struct.CThostFtdcDepthMarketDataField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcInputOrderActionField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcInputOrderField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcRspUserLoginField;
import com.nabiki.ctp4j.md.CThostFtdcMdSpi;
import com.nabiki.ctp4j.trader.CThostFtdcTraderSpi;

import java.util.HashSet;
import java.util.Set;

public class TradeBook extends CThostFtdcMdSpi {
    private final static TradeBook book = new TradeBook();
    private final CThostFtdcRspUserLoginField loginProfile
            = new CThostFtdcRspUserLoginField();
    private final Set<CThostFtdcTraderSpi> spiSet = new HashSet<>();

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

    public void addSPI(CThostFtdcTraderSpi spi) {
        this.spiSet.add(spi);
    }

    public void removeSPI(CThostFtdcTraderSpi spi) {
        this.spiSet.remove(spi);
    }

    public int enqueue(CThostFtdcInputOrderField order, int requestID) {
        return 0;
    }

    public int enqueue(CThostFtdcInputOrderActionField action, int requestID) {
        return 0;
    }

    @Override
    public void OnRtnDepthMarketData(
            CThostFtdcDepthMarketDataField depthMarketData) {
    }
}
