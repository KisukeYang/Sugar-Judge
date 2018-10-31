package com.sugar.judge.entity.kotlin

import com.sugar.judge.entity.Contest
import com.sugar.judge.entity.ContestProblem
import com.sugar.judge.entity.ContestUser

fun Contest(
  autoStart: Boolean? = null,
  commend: String? = null,
  contestName: String? = null,
  contestStatus: String? = null,
  contestUsers: Iterable<com.sugar.judge.entity.ContestUser>? = null,
  createTime: String? = null,
  creator: String? = null,
  description: String? = null,
  disuse: Boolean? = null,
  hidden: Boolean? = null,
  id: Int? = null,
  problems: Iterable<com.sugar.judge.entity.ContestProblem>? = null,
  startTime: String? = null,
  timeLimit: String? = null,
  updateTime: String? = null): Contest = com.sugar.judge.entity.Contest().apply {

  if (autoStart != null) {
    this.setAutoStart(autoStart)
  }
  if (commend != null) {
    this.setCommend(commend)
  }
  if (contestName != null) {
    this.setContestName(contestName)
  }
  if (contestStatus != null) {
    this.setContestStatus(contestStatus)
  }
  if (contestUsers != null) {
    this.setContestUsers(contestUsers.toList())
  }
  if (createTime != null) {
    this.setCreateTime(createTime)
  }
  if (creator != null) {
    this.setCreator(creator)
  }
  if (description != null) {
    this.setDescription(description)
  }
  if (disuse != null) {
    this.setDisuse(disuse)
  }
  if (hidden != null) {
    this.setHidden(hidden)
  }
  if (id != null) {
    this.setId(id)
  }
  if (problems != null) {
    this.setProblems(problems.toList())
  }
  if (startTime != null) {
    this.setStartTime(startTime)
  }
  if (timeLimit != null) {
    this.setTimeLimit(timeLimit)
  }
  if (updateTime != null) {
    this.setUpdateTime(updateTime)
  }
}

