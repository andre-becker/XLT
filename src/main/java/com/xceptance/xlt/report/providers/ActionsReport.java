package com.xceptance.xlt.report.providers;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * 
 */
@XStreamAlias("actions")
public class ActionsReport
{
    @XStreamImplicit
    public List<TimerReport> actions = new ArrayList<TimerReport>();
}
