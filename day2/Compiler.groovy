package jp.orto.experiment.os30day.day2

import static java.lang.Math.min

File BASE_DIR = new File("/Documents/my/2009/Orto2/Experiment/Experiment/os30day/day2")
File lstFile = new File(BASE_DIR, "helloos.lst")
File imgFile = new File(BASE_DIR, "helloos.img")
File outJsFile = new File(BASE_DIR, "helloos.js")
File videoBiosJsFile = new File(BASE_DIR, "videoBios.js")

List<String> lines = lstFile.readLines("MS932")

int offset = 0

List ops = []
for (String line in lines) {
  if (line.size() < 6) break

  def op = [:]

  op.idx = -1
  try {
    op.idx = line[0..5] as int
  } catch (Exception e) {
  }

  op.offset = line[7..<15].trim()
  op.bytes = line[16..<min(line.size(), 46)].split(" ")

  op.isData = op.idx == -1
  if (line.size() >= 48) {
    String opcode = line[48..<line.size()].trim()

    // remove comment
    int commentIdx = opcode.indexOf(";")
    if (commentIdx != -1) {
      opcode = opcode[0..<commentIdx].trim()
    }
    op.opcode = opcode;

    op.isData = opcode.startsWith("DB") || opcode.startsWith("DW") || opcode.startsWith("DD") ||
        opcode.startsWith("RESB")

    if (opcode.startsWith("ORG")) {
      offset = parseDigits(opcode.substring(4).trim().substring(2))
    }
  }

  ops << op
}

StringBuilder srcSB = new StringBuilder()
srcSB << """(function() {
var AX = 0, BX = 0, CX = 0, DX = 0, SP = 0, BP = 0, SI = 0, DI = 0;
var ES = 0, CS = 0, SS = 0, DS = 0, FS = 0, GS = 0;
var ZF = false;
var ip = ${offset};

function main() {
  while (true) {
    switch (ip) {
      case ${offset}:
"""

String prevOffset = null;

for (def op in ops) {
//  println op

  if (!op.isData && op.bytes.size() > 0) {
    srcSB << "        "
    switch (op.bytes[0]) {
      case "3C": srcSB << "ZF = (AX & 0xFF) == ${parseDigits(op.bytes[1]) & 0xFF};\n"; break;

      case "74":
        srcSB << "if (ZF) { ip = ${jumpAddr(op.offset, op.bytes[1])}; break; }\n"
        break

      case "83":
        switch (op.bytes[1]) {
          case "C6": srcSB << "SI += ${(short) parseDigits(op.bytes[2])};\n"; break;
          default: assert false;
        }
        break

      case "8A":
        switch (op.bytes[1]) {
          case "04": srcSB << "AX = (AX & 0xFF00) | bytes[SI - ${offset}];\n"; break;
          default: assert false;
        }
        break

      case "8E":
        switch (op.bytes[1]) {
          case "C0": srcSB << "ES = AX;\n"; break;
          case "D0": srcSB << "SS = AX;\n"; break;
          case "D8": srcSB << "DS = AX;\n"; break;
          default: assert false;
        }
        break

      case "B0": srcSB << "AX = (AX & 0xFF00) | ${parseDigits(op.bytes[1]) & 0xFF};\n"; break;
      case "B1": srcSB << "CX = (CX & 0xFF00) | ${parseDigits(op.bytes[1]) & 0xFF};\n"; break;
      case "B2": srcSB << "DX = (DX & 0xFF00) | ${parseDigits(op.bytes[1]) & 0xFF};\n"; break;
      case "B3": srcSB << "BX = (BX & 0xFF00) | ${parseDigits(op.bytes[1]) & 0xFF};\n"; break;
      case "B4": srcSB << "AX = (AX & 0xFF) | ${(parseDigits(op.bytes[1]) & 0xFF) << 8};\n"; break;
      case "B5": srcSB << "CX = (CX & 0xFF) | ${(parseDigits(op.bytes[1]) & 0xFF) << 8};\n"; break;
      case "B6": srcSB << "DX = (DX & 0xFF) | ${(parseDigits(op.bytes[1]) & 0xFF) << 8};\n"; break;
      case "B7": srcSB << "BX = (BX & 0xFF) | ${(parseDigits(op.bytes[1]) & 0xFF) << 8};\n"; break;
      case "B8": srcSB << "AX = ${parseDigits(op.bytes[1])};\n"; break;
      case "B9": srcSB << "CX = ${parseDigits(op.bytes[1])};\n"; break;
      case "BA": srcSB << "DX = ${parseDigits(op.bytes[1])};\n"; break;
      case "BB": srcSB << "BX = ${parseDigits(op.bytes[1])};\n"; break;
      case "BC": srcSB << "SP = ${parseDigits(op.bytes[1])};\n"; break;
      case "BD": srcSB << "BP = ${parseDigits(op.bytes[1])};\n"; break;
      case "BE": srcSB << "SI = ${parseDigits(op.bytes[1])};\n"; break;
      case "BF": srcSB << "DI = ${parseDigits(op.bytes[1])};\n"; break;

      case "CD":
        switch (parseDigits(op.bytes[1])) {
          case 0x10: srcSB << "videoBios(AX, BX);\n"; break;
          default: assert false;
        }
        break;

      case "EB": srcSB << "ip = ${jumpAddr(op.offset, op.bytes[1])}; break;\n"; break;

      case "F4": srcSB << "return;\n"; break;

      default: assert false, "op.bytes[0] = " + op.bytes[0]
    }
  }
  if (!op.isData && op.bytes.size() == 0 && op.offset.size() > 0) {
    if (op.offset != prevOffset && op.opcode.indexOf(":") != -1) {
      srcSB << "      case ${parseDigits(op.offset)}:\n"
      prevOffset = op.offset
    }
  }
}

srcSB << """    }
  }
}
window.onload = main;

"""

srcSB << videoBiosJsFile.getText("UTF-8") << "\n";

srcSB << "var bytes = [";
for (byte b in imgFile.bytes) {
  srcSB << (b & 0xFF) << ",";
}
srcSB << "];\n"

srcSB << "})();\n"

//println srcSB

outJsFile.setText(srcSB as String, "UTF-8")

int jumpAddr(offset, diff) {
  parseDigits(offset) + 2 + (byte) parseDigits(diff)
}

int parseDigits(String s) {
  Integer.parseInt(s, 16)
}


