package com.sugar.judge.entity.kotlin

import com.sugar.judge.entity.UserInfo

fun UserInfo(
  ac: Int? = null,
  acRatio: Double? = null,
  ce: Int? = null,
  mle: Int? = null,
  rankBefore: Int? = null,
  rankNow: Int? = null,
  rankPoint: Int? = null,
  re: Int? = null,
  submit: Int? = null,
  tle: Int? = null,
  wa: Int? = null): UserInfo = com.sugar.judge.entity.UserInfo().apply {

  if (ac != null) {
    this.setAc(ac)
  }
  if (acRatio != null) {
    this.setAcRatio(acRatio)
  }
  if (ce != null) {
    this.setCe(ce)
  }
  if (mle != null) {
    this.setMle(mle)
  }
  if (rankBefore != null) {
    this.setRankBefore(rankBefore)
  }
  if (rankNow != null) {
    this.setRankNow(rankNow)
  }
  if (rankPoint != null) {
    this.setRankPoint(rankPoint)
  }
  if (re != null) {
    this.setRe(re)
  }
  if (submit != null) {
    this.setSubmit(submit)
  }
  if (tle != null) {
    this.setTle(tle)
  }
  if (wa != null) {
    this.setWa(wa)
  }
}

