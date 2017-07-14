/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.sql;

import org.apache.calcite.avatica.util.TimeUnitRange;

/**
 * <code>DialectUnparseMysql</code> defines how a <code>SqlOperator</code> should be unparsed
 * for execution against a Mysql database. It reverts to the unparse method of the operator
 * if this database's implementation is standard.
 */
public class DialectUnparseMysql implements SqlDialect.DialectUnparser {
  public void unparseCall(
      SqlOperator operator,
      SqlWriter writer,
      SqlCall call,
      int leftPrec,
      int rightPrec) {
    switch (operator.getKind()) {
    case FLOOR:
      if (call.operandCount() != 2) {
        operator.unparse(writer, call, leftPrec, rightPrec);
        return;
      }

      unparseFloor(writer, call);
      break;

    default:
      operator.unparse(writer, call, leftPrec, rightPrec);
    }
  }

  /**
   * Unparse datetime floor for MySQL. There is no TRUNC function, so simulate this
   * using calls to DATE_FORMAT.
   *
   * @param writer SqlWriter
   * @param call SqlCall
   */
  private void unparseFloor(SqlWriter writer, SqlCall call) {
    SqlLiteral node = call.operand(1);
    TimeUnitRange unit = (TimeUnitRange) node.getValue();

    if (unit == TimeUnitRange.WEEK) {
      writer.print("STR_TO_DATE");
      SqlWriter.Frame frame = writer.startList("(", ")");

      writer.print("DATE_FORMAT(");
      call.operand(0).unparse(writer, 0, 0);
      writer.print(", '%x%v-1'), '%x%v-%w'");
      writer.endList(frame);
      return;
    }

    String format;
    switch (unit) {
    case YEAR:
      format = "%Y-01-01";
      break;
    case MONTH:
      format = "%Y-%m-01";
      break;
    case DAY:
      format = "%Y-%m-%d";
      break;
    case HOUR:
      format = "%Y-%m-%d %k:00:00";
      break;
    case MINUTE:
      format = "%Y-%m-%d %k:%i:00";
      break;
    case SECOND:
      format = "%Y-%m-%d %k:%i:%s";
      break;
    default:
      throw new AssertionError("MYSQL does not support FLOOR for time unit: "
          + unit);
    }

    writer.print("DATE_FORMAT");
    SqlWriter.Frame frame = writer.startList("(", ")");
    call.operand(0).unparse(writer, 0, 0);
    writer.sep(",", true);
    writer.print("'" + format + "'");
    writer.endList(frame);
  }
}

// End DialectUnparseMysql.java
