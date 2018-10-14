/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.beans;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author pavankumar.g
 */
public class Test {

    public static void main(String[] args) {

        ArrayList<MatchedResultsBean> arraylist = new ArrayList<MatchedResultsBean>();
        arraylist.add(new MatchedResultsBean(19, "Zues"));
        arraylist.add(new MatchedResultsBean(27, "Abey"));
        arraylist.add(new MatchedResultsBean(8, "Vignesh"));

        /* Sorting on Rollno property*/
        System.out.println("RollNum Sorting:");
        Collections.sort(arraylist, MatchedResultsBean.Score);
        for (MatchedResultsBean str : arraylist) {
            System.out.println(str);
        }
    }
}
