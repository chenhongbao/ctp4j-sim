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
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Random;

public class TickBookTest {
    private final static String dir = "C:\\Users\\chenh\\Desktop\\";

    private final static CThostFtdcDepthMarketDataField originMd;
    static {
        originMd = new CThostFtdcDepthMarketDataField();
        originMd.InstrumentID = "x2009";
        originMd.AskVolume1 = 1352;
        originMd.AskPrice1 = 2100;
        originMd.BidVolume1 = 465;
        originMd.BidPrice1 = 2099;
        originMd.PreClosePrice = 2099;
        originMd.PreSettlementPrice = 2104;
    }

    @Test
    public void basic() {
        var book = new TickBook(originMd, 1.0D);
        for (int i = 0; i < 1000 * 10; ++i)
            write(book.refresh().LastPrice, "basic.txt");
    }

    private static final Random random = new Random(TickBook.class.hashCode());

    private double stable_buy_chance() {
        var origin = random.nextDouble();
        return (origin - 0.5D) * random.nextDouble() + 0.5D;
    }

    @Test
    public void stable_market() {
        var book = new TickBook(originMd, 1.0D);

        for (int j = 0; j < 10; ++j) {
            book.setBuyChance(stable_buy_chance());
            for (int i = 0; i < 1000; ++i)
                write(book.refresh().LastPrice, "stable.txt");
        }
    }

    private double shaking_buy_chance() {
        var origin = random.nextDouble();
        var transform = (0.5D - Math.abs(0.5D - origin)) * random.nextDouble();
        if (origin > 0.5D)
            return origin + transform;
        else if (origin < 0.5D)
            return origin - transform;
        else
            return origin;
    }

    @Test
    public void shaking_market() {
        var book = new TickBook(originMd, 1.0D);

        for (int j = 0; j < 10; ++j) {
            book.setBuyChance(shaking_buy_chance());
            for (int i = 0; i < 1000; ++i)
                write(book.refresh().LastPrice, "shaking.txt");
        }
    }

    @Test
    public void biased_market_up() {
        var book = new TickBook(originMd, 1.0D);

        for (int j = 0; j < 10; ++j) {
            book.setBuyChance(0.55);
            for (int i = 0; i < 1000; ++i)
                write(book.refresh().LastPrice, "biased_up.txt");
        }
    }

    @Test
    public void biased_market_down() {
        var book = new TickBook(originMd, 1.0D);

        for (int j = 0; j < 10; ++j) {
            book.setBuyChance(0.45);
            for (int i = 0; i < 1000; ++i)
                write(book.refresh().LastPrice, "biased_down.txt");
        }
    }

    private static void write(double price, String file) {
        while (true)
            try (PrintWriter pw = new PrintWriter(
                    new FileWriter(Path.of(dir, file).toFile(),
                            true))) {
                pw.println(price);
                pw.flush();
                break;
            } catch (IOException ignored) {
            }
    }
}