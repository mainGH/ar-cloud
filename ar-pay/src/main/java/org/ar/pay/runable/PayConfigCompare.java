package org.ar.pay.runable;

import org.ar.pay.entity.PayConfig;

import java.util.Comparator;

public class PayConfigCompare implements Comparator<PayConfig> {
    public int compare(PayConfig o1,PayConfig o2){
        return o1.getRate().compareTo(o2.getRate());
    }
}
