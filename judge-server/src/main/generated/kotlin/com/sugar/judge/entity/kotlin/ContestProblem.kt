package com.sugar.judge.entity.kotlin

import com.sugar.judge.entity.ContestProblem

fun ContestProblem(
  accept: Int? = null,
  contestId: Int? = null,
  problemId: Int? = null,
  submit: Int? = null,
  title: String? = null): ContestProblem = com.sugar.judge.entity.ContestProblem().apply {

  if (accept != null) {
    this.setAccept(accept)
  }
  if (contestId != null) {
    this.setContestId(contestId)
  }
  if (problemId != null) {
    this.setProblemId(problemId)
  }
  if (submit != null) {
    this.setSubmit(submit)
  }
  if (title != null) {
    this.setTitle(title)
  }
}

