package com.sugar.judge.entity.kotlin

import com.sugar.judge.entity.User

fun User(
  ac: Int? = null,
  acRatio: Double? = null,
  ce: Int? = null,
  email: String? = null,
  enabled: Boolean? = null,
  id: Int? = null,
  ipInfo: String? = null,
  language: String? = null,
  mle: Int? = null,
  nickname: String? = null,
  password: String? = null,
  rankBefore: Int? = null,
  rankNow: Int? = null,
  rankPoint: Int? = null,
  re: Int? = null,
  registerTime: String? = null,
  role: String? = null,
  submit: Int? = null,
  tle: Int? = null,
  username: String? = null,
  wa: Int? = null): User = com.sugar.judge.entity.User().apply {

  if (ac != null) {
    this.setAc(ac)
  }
  if (acRatio != null) {
    this.setAcRatio(acRatio)
  }
  if (ce != null) {
    this.setCe(ce)
  }
  if (email != null) {
    this.setEmail(email)
  }
  if (enabled != null) {
    this.setEnabled(enabled)
  }
  if (id != null) {
    this.setId(id)
  }
  if (ipInfo != null) {
    this.setIpInfo(ipInfo)
  }
  if (language != null) {
    this.setLanguage(language)
  }
  if (mle != null) {
    this.setMle(mle)
  }
  if (nickname != null) {
    this.setNickname(nickname)
  }
  if (password != null) {
    this.setPassword(password)
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
  if (registerTime != null) {
    this.setRegisterTime(registerTime)
  }
  if (role != null) {
    this.setRole(role)
  }
  if (submit != null) {
    this.setSubmit(submit)
  }
  if (tle != null) {
    this.setTle(tle)
  }
  if (username != null) {
    this.setUsername(username)
  }
  if (wa != null) {
    this.setWa(wa)
  }
}

