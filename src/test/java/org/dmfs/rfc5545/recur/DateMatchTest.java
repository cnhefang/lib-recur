/*
 * Copyright 2021 Marten Gajda <marten@dmfs.org>
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dmfs.rfc5545.recur;

import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.Weekday;
import org.junit.Test;

import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class DateMatchTest {
    @Test
    public void DateMatchTestWeekly() {
        var match = DateMatchRule("FREQ=WEEKLY;WKST=MO;BYDAY=FR,SU;UNTIL=20220430T170000Z", DateTime.parse("GMT+8", "20211215T160000Z"), DateTime.parse("20211217"));
        assertTrue("Not match", match);
    }

    @Test
    public void DateMatchTestWeekly2() {
        var match = DateMatchRule("FREQ=WEEKLY;WKST=MO;UNTIL=20220430T170000Z", DateTime.parse("GMT+8", "20211215T160000Z"), DateTime.parse("20211222"));
        assertTrue("Not match", match);
    }

    @Test
    public void DateMatchTestDaily() {
        var match = DateMatchRule("FREQ=DAILY;WKST=MO;BYDAY=1,15,17,SU;UNTIL=20220430T170000Z", DateTime.parse("GMT+8", "20211215T160000Z"), DateTime.parse("20211217"));
        assertTrue("Not match", match);
    }

    @Test
    public void DateMatchTestDaily2() {
        var match = DateMatchRule("FREQ=DAILY;WKST=MO;UNTIL=20220430T170000Z", DateTime.parse("GMT+8", "20211215T160000Z"), DateTime.parse("20211222"));
        assertTrue("Not match", match);
    }

    @Test
    public void DateMatchTestDaily3() {
        var match = DateMatchRule("FREQ=DAILY;WKST=MO;UNTIL=20210430T180000Z", DateTime.parse("GMT+8", "20211215T160000Z"), DateTime.parse("20211222"));
        assertTrue("Not match", match);
    }

    @Test
    public void DateMatchTestMonthly() {
        var match = DateMatchRule("FREQ=MONTHLY;WKST=MO;BYMONTHDAY=1,15,17,SU;UNTIL=20230430T170000Z", DateTime.parse("GMT+8", "20211215T160000Z"), DateTime.parse("20221217"));
        assertTrue("Not match", match);
    }

    @Test
    public void DateMatchTestMonthly2() {
        var match = DateMatchRule("FREQ=MONTHLY;WKST=MO;UNTIL=20220430T170000Z", DateTime.parse("GMT+8", "20211215T160000Z"), DateTime.parse("20220215"));
        assertTrue("Not match", match);
    }

    @Test
    public void DateMatchTestMonthly3() {
        var match = DateMatchRule("FREQ=MONTHLY;WKST=MO;UNTIL=20220430T170000Z", DateTime.parse("GMT+8", "20211215T160000Z"), DateTime.parse("20211215"));
        assertTrue("Not match", match);
    }

    @Test
    public void DateMatchTestYearly() {
        var rule = "FREQ=YEARLY;WKST=MO;BYMONTH=1,5,7,SU;UNTIL=20230430T170000Z";
        var match = DateMatchRule(rule, DateTime.parse("GMT+8", "20211215T160000Z"), DateTime.parse("20221216"));
        assertTrue("Not match", match);
        match = DateMatchRule(rule, DateTime.parse("GMT+8", "20211215T160000Z"), DateTime.parse("20220116"));
        assertTrue("Not match", match);
        match = DateMatchRule(rule, DateTime.parse("GMT+8", "20211215T160000Z"), DateTime.parse("20220716"));
        assertTrue("Not match", match);
    }

    @Test
    public void DateMatchTestYearly2() {
        var rule = "FREQ=YEARLY;WKST=MO;UNTIL=20250430T170000Z";
        var match = DateMatchRule(rule, DateTime.parse(TimeZone.getTimeZone("GMT+8").getID(), "20211215T160000Z"), DateTime.parse("20220216"));
        assertTrue("Not match", match);
    }

    @Test
    public void DateMatchTestYearly3() {
        var rule = "FREQ=YEARLY;WKST=MO;UNTIL=20320430T170000Z";
        var match = DateMatchRule(rule, DateTime.parse("GMT+8", "20211215T160000Z"), DateTime.parse("20211215"));
        assertTrue("Not match", match);
    }

    public boolean DateMatchRule(String ruleExpression, DateTime startTime, DateTime dateTime) {
        if (startTime.getYear() == dateTime.getYear() && startTime.getMonth() == dateTime.getMonth() && startTime.getDayOfMonth() == dateTime.getDayOfMonth()) {
            return true;
        }
        try {
            var rule = new RecurrenceRule(ruleExpression);

            var startOfDay = startTime.startOfDay();
            if (startOfDay.after(dateTime)) {
                return false;
            }
            if (rule.getUntil() != null && rule.getUntil().before(dateTime)) {
                return false;
            }
            if (rule.getFreq().equals(Freq.DAILY)) {
                return true;
            }

            RecurrenceRuleIterator it = rule.iterator(startTime);

            int maxInstances = 1000; // limit instances for rules that recur forever

            while (it.hasNext() && (!rule.isInfinite() || maxInstances-- > 0)) {
                DateTime nextInstance = it.nextDateTime();
                Logger.getAnonymousLogger().info(nextInstance.toString());
                // do something with nextInstance
            }
            if (rule.getFreq().equals(Freq.WEEKLY)) {

                var parts = rule.getByDayPart();
                if (parts == null || parts.size() == 0) {
                    return dateTime.getDayOfWeek() == startTime.getDayOfWeek();
                }
                var partsDay = parts.stream().map(x -> x.weekday.ordinal()).collect(Collectors.toList());
                var dayOfWeek = dateTime.getDayOfWeek();

                if (partsDay.contains(dayOfWeek)) {
                    return true;
                }
            }
            if (rule.getFreq().equals(Freq.MONTHLY)) {
                var parts = rule.getByPart(RecurrenceRule.Part.BYMONTHDAY);
                if (parts == null || parts.size() == 0) {
                    return dateTime.getDayOfMonth() == startTime.getDayOfMonth();
                }
                var dayOfMonth = dateTime.getDayOfMonth();
                if (parts.contains(Integer.valueOf(dayOfMonth))) {
                    return true;
                }
            }
            if (rule.getFreq().equals(Freq.YEARLY)) {
                var parts = rule.getByPart(RecurrenceRule.Part.BYMONTH);
                var monthOfYear = dateTime.getMonth();
                if (parts == null || parts.size() == 0) {
                    return startTime.getMonth() == dateTime.getMonth() && startTime.getDayOfMonth() == dateTime.getDayOfMonth();
                }
                if (parts.contains(Integer.valueOf(monthOfYear)) && startTime.getDayOfMonth() == dateTime.getDayOfMonth()) {
                    return true;
                }
            }

        } catch (InvalidRecurrenceRuleException e) {

            e.printStackTrace();
            return false;
        }

        return false;
    }
}
