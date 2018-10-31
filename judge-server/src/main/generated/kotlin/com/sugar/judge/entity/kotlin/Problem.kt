package com.sugar.judge.entity.kotlin

import com.sugar.judge.entity.Problem

/**
 * A function providing a DSL for building [com.sugar.judge.entity.Problem] objects.
 *
 * 类描述： 试题 entity
 *
 * @param acNum 
 * @param acRatio 
 * @param alterOutData 
 * @param author 
 * @param background 
 * @param clickNum 
 * @param code 
 * @param conditionalJudger 
 * @param content 
 * @param difficulty 
 * @param disuse 
 * @param hidden 
 * @param hint 
 * @param id 
 * @param inFilePath 
 * @param insertTime 
 * @param memoryLimit 
 * @param outFilePath 
 * @param sampleInput 
 * @param sampleOutput 
 * @param submitNum 
 * @param tab 
 * @param theInput 
 * @param theOutput 
 * @param timeLimit 
 * @param title 
 * @param type 
 * @param updateTime 
 *
 * <p/>
 * NOTE: This function has been automatically generated from the [com.sugar.judge.entity.Problem original] using Vert.x codegen.
 */
fun Problem(
  acNum: Int? = null,
  acRatio: Double? = null,
  alterOutData: String? = null,
  author: String? = null,
  background: String? = null,
  clickNum: Int? = null,
  code: String? = null,
  conditionalJudger: String? = null,
  content: String? = null,
  difficulty: Int? = null,
  disuse: Boolean? = null,
  hidden: Boolean? = null,
  hint: String? = null,
  id: Int? = null,
  inFilePath: String? = null,
  insertTime: String? = null,
  memoryLimit: Int? = null,
  outFilePath: String? = null,
  sampleInput: String? = null,
  sampleOutput: String? = null,
  submitNum: Int? = null,
  tab: String? = null,
  theInput: String? = null,
  theOutput: String? = null,
  timeLimit: Int? = null,
  title: String? = null,
  type: String? = null,
  updateTime: String? = null): Problem = com.sugar.judge.entity.Problem().apply {

  if (acNum != null) {
    this.setAcNum(acNum)
  }
  if (acRatio != null) {
    this.setAcRatio(acRatio)
  }
  if (alterOutData != null) {
    this.setAlterOutData(alterOutData)
  }
  if (author != null) {
    this.setAuthor(author)
  }
  if (background != null) {
    this.setBackground(background)
  }
  if (clickNum != null) {
    this.setClickNum(clickNum)
  }
  if (code != null) {
    this.setCode(code)
  }
  if (conditionalJudger != null) {
    this.setConditionalJudger(conditionalJudger)
  }
  if (content != null) {
    this.setContent(content)
  }
  if (difficulty != null) {
    this.setDifficulty(difficulty)
  }
  if (disuse != null) {
    this.setDisuse(disuse)
  }
  if (hidden != null) {
    this.setHidden(hidden)
  }
  if (hint != null) {
    this.setHint(hint)
  }
  if (id != null) {
    this.setId(id)
  }
  if (inFilePath != null) {
    this.setInFilePath(inFilePath)
  }
  if (insertTime != null) {
    this.setInsertTime(insertTime)
  }
  if (memoryLimit != null) {
    this.setMemoryLimit(memoryLimit)
  }
  if (outFilePath != null) {
    this.setOutFilePath(outFilePath)
  }
  if (sampleInput != null) {
    this.setSampleInput(sampleInput)
  }
  if (sampleOutput != null) {
    this.setSampleOutput(sampleOutput)
  }
  if (submitNum != null) {
    this.setSubmitNum(submitNum)
  }
  if (tab != null) {
    this.setTab(tab)
  }
  if (theInput != null) {
    this.setTheInput(theInput)
  }
  if (theOutput != null) {
    this.setTheOutput(theOutput)
  }
  if (timeLimit != null) {
    this.setTimeLimit(timeLimit)
  }
  if (title != null) {
    this.setTitle(title)
  }
  if (type != null) {
    this.setType(type)
  }
  if (updateTime != null) {
    this.setUpdateTime(updateTime)
  }
}

