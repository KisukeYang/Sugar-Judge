package com.sugar.judge.entity.kotlin

import com.sugar.judge.entity.Submission

fun Submission(
  codeLock: String? = null,
  color: String? = null,
  contest: Int? = null,
  contestId: Int? = null,
  contestTime: Int? = null,
  errMsg: String? = null,
  executeTime: Long? = null,
  id: Int? = null,
  ipFrom: String? = null,
  language: String? = null,
  nickName: String? = null,
  problemCode: String? = null,
  problemId: Int? = null,
  problemTitle: String? = null,
  status: String? = null,
  submitTime: String? = null,
  userId: Int? = null): Submission = com.sugar.judge.entity.Submission().apply {

  if (codeLock != null) {
    this.setCodeLock(codeLock)
  }
  if (color != null) {
    this.setColor(color)
  }
  if (contest != null) {
    this.setContest(contest)
  }
  if (contestId != null) {
    this.setContestId(contestId)
  }
  if (contestTime != null) {
    this.setContestTime(contestTime)
  }
  if (errMsg != null) {
    this.setErrMsg(errMsg)
  }
  if (executeTime != null) {
    this.setExecuteTime(executeTime)
  }
  if (id != null) {
    this.setId(id)
  }
  if (ipFrom != null) {
    this.setIpFrom(ipFrom)
  }
  if (language != null) {
    this.setLanguage(language)
  }
  if (nickName != null) {
    this.setNickName(nickName)
  }
  if (problemCode != null) {
    this.setProblemCode(problemCode)
  }
  if (problemId != null) {
    this.setProblemId(problemId)
  }
  if (problemTitle != null) {
    this.setProblemTitle(problemTitle)
  }
  if (status != null) {
    this.setStatus(status)
  }
  if (submitTime != null) {
    this.setSubmitTime(submitTime)
  }
  if (userId != null) {
    this.setUserId(userId)
  }
}

