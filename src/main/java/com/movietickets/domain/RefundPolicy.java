package com.movietickets.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "refund_policies")
public class RefundPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer hoursBeforeShow;

    @Column(nullable = false)
    private Integer refundPercentage;

    protected RefundPolicy() {
    }

    public RefundPolicy(Integer hoursBeforeShow, Integer refundPercentage) {
        this.hoursBeforeShow = hoursBeforeShow;
        this.refundPercentage = refundPercentage;
    }

    public Long getId() {
        return id;
    }

    public Integer getHoursBeforeShow() {
        return hoursBeforeShow;
    }

    public Integer getRefundPercentage() {
        return refundPercentage;
    }

    public void setRefundPercentage(Integer refundPercentage) {
        this.refundPercentage = refundPercentage;
    }
}
