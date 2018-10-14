/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.beans;

import java.util.Comparator;

/**
 *
 * @author pavankumar.g
 */
public class MatchedResultsBean {

    private String id;
    private int score;

    public MatchedResultsBean(int score, String id) {
        this.score = score;
        this.id = id;
    }

    /*Comparator for sorting the list by score*/
    public static Comparator<MatchedResultsBean> Score = new Comparator<MatchedResultsBean>() {

        public int compare(MatchedResultsBean s1, MatchedResultsBean s2) {

            int score1 = s1.getScore();
            int score2 = s2.getScore();

            /*For ascending order*/
//            return score1 - score2;

            /*For descending order*/
            return score2 - score1;
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "MatchedResultsBean{" + "id=" + id + ", score=" + score + '}';
    }
}
