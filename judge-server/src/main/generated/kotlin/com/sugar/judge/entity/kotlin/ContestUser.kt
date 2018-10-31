package com.sugar.judge.entity.kotlin

import com.sugar.judge.entity.ContestUser

fun ContestUser(
  acList: String? = null,
  accept: Int? = null,
  contestId: Int? = null,
  nickName: String? = null,
  penalty: Int? = null,
  submits: Int? = null,
  userId: Int? = null): ContestUser = com.sugar.judge.entity.ContestUser().apply {

  if (acList != null) {
    this.setAcList(acList)
  }
  if (accept != null) {
    this.setAccept(accept)
  }
  if (contestId != null) {
    this.setContestId(contestId)
  }
  if (nickName != null) {
    this.setNickName(nickName)
  }
  if (penalty != null) {
    this.setPenalty(penalty)
  }
  if (submits != null) {
    this.setSubmits(submits)
  }
  if (userId != null) {
    this.setUserId(userId)
  }
}

