let processor = window['processor'].net.akehurst.language.processor.processor
function doScan() {
  let grammarStr = document.getElementById("grammar").value
  let sentence = document.getElementById("sentence").value

  let proc = processor(grammarStr)
  let tokens = proc.scan(sentence)
  let tokStr = JSON.stringify(tokens)
  document.getElementById("output").value = tokStr
}

function doParse() {
  let grammarStr = document.getElementById("grammar").value
  let goal = document.getElementById("goal").value
  let sentence = document.getElementById("sentence").value

  let proc = processor(grammarStr)
  let tree = proc.parse(goal,sentence)
  let str = tree.toStringAllWithIndent('  ')
  document.getElementById("output").value = str
}