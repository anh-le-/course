package edu.berkeley.cs186.database;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.berkeley.cs186.database.StudentTest;
import edu.berkeley.cs186.database.datatypes.DataType;
import edu.berkeley.cs186.database.datatypes.FloatDataType;
import edu.berkeley.cs186.database.datatypes.IntDataType;
import edu.berkeley.cs186.database.datatypes.StringDataType;
import edu.berkeley.cs186.database.query.QueryPlan;
import edu.berkeley.cs186.database.query.QueryPlanException;
import edu.berkeley.cs186.database.table.Record;
import edu.berkeley.cs186.database.table.Schema;

import static org.junit.Assert.*;

public class TestDatabaseQueries {
  private static Database database;
  private Database.Transaction transaction;

  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  @Rule
  public Timeout globalTimeout = Timeout.seconds(10); // 10 seconds max per method tested

  @BeforeClass
  public static void setupClass() throws DatabaseException, IOException {
    File tempDir = tempFolder.newFolder("myDb", "school");
    database = new Database(tempDir.getAbsolutePath());

    createSchemas();
    readTuplesFromFiles();
  }

  @Before
  public void setup() throws DatabaseException {
    this.transaction = database.beginTransaction();
  }

  @After
  public void teardown() throws DatabaseException {
    this.transaction.end();
  }

  @Test
  public void testJoinStudentNamesWithClassNames() throws DatabaseException, QueryPlanException {
    this.transaction.queryAs("Students", "S");
    this.transaction.queryAs("Enrollments", "E");
    this.transaction.queryAs("Courses", "C");

    QueryPlan queryPlan = this.transaction.query("S");
    queryPlan.join("E", "S.sid", "E.sid");
    queryPlan.join("C", "E.cid", "C.cid");
    List<String> columns = new ArrayList<String>();
    columns.add("S.name");
    columns.add("C.name");
    queryPlan.select(columns);

    Iterator<Record> recordIterator = queryPlan.execute();

    int count = 0;
    while (recordIterator.hasNext()) {
      recordIterator.next();
      count++;
    }

    assertEquals(1000, count);
  }

  private static void createSchemas() throws DatabaseException {
    List<String> studentSchemaNames = new ArrayList<String>();
    studentSchemaNames.add("sid");
    studentSchemaNames.add("name");
    studentSchemaNames.add("major");
    studentSchemaNames.add("gpa");

    List<DataType> studentSchemaTypes = new ArrayList<DataType>();
    studentSchemaTypes.add(new IntDataType());
    studentSchemaTypes.add(new StringDataType(20));
    studentSchemaTypes.add(new StringDataType(20));
    studentSchemaTypes.add(new FloatDataType());

    Schema studentSchema = new Schema(studentSchemaNames, studentSchemaTypes);

    database.createTable(studentSchema, "Students");

    List<String> courseSchemaNames = new ArrayList<String>();
    courseSchemaNames.add("cid");
    courseSchemaNames.add("name");
    courseSchemaNames.add("department");

    List<DataType> courseSchemaTypes = new ArrayList<DataType>();
    courseSchemaTypes.add(new IntDataType());
    courseSchemaTypes.add(new StringDataType(20));
    courseSchemaTypes.add(new StringDataType(20));

    Schema courseSchema = new Schema(courseSchemaNames, courseSchemaTypes);

    database.createTable(courseSchema, "Courses");

    List<String> enrollmentSchemaNames = new ArrayList<String>();
    enrollmentSchemaNames.add("sid");
    enrollmentSchemaNames.add("cid");

    List<DataType> enrollmentSchemaTypes = new ArrayList<DataType>();
    enrollmentSchemaTypes.add(new IntDataType());
    enrollmentSchemaTypes.add(new IntDataType());

    Schema enrollmentSchema = new Schema(enrollmentSchemaNames, enrollmentSchemaTypes);

    database.createTable(enrollmentSchema, "Enrollments");
  }

  private static void readTuplesFromFiles() throws DatabaseException, IOException {
    Database.Transaction transaction = database.beginTransaction();

    // read student tuples
    List<String> studentLines = Files.readAllLines(Paths.get("students.csv"), Charset.defaultCharset());

    for (String line : studentLines) {
      String[] splits = line.split(",");
      List<DataType> values = new ArrayList<DataType>();

      values.add(new IntDataType(Integer.parseInt(splits[0])));
      values.add(new StringDataType(splits[1].trim(), 20));
      values.add(new StringDataType(splits[2].trim(), 20));
      values.add(new FloatDataType(Float.parseFloat(splits[3])));

      transaction.addRecord("Students", values);
    }

    List<String> courseLines = Files.readAllLines(Paths.get("courses.csv"), Charset.defaultCharset());

    for (String line : courseLines) {
      String[] splits = line.split(",");
      List<DataType> values = new ArrayList<DataType>();

      values.add(new IntDataType(Integer.parseInt(splits[0])));
      values.add(new StringDataType(splits[1].trim(), 20));
      values.add(new StringDataType(splits[2].trim(), 20));

      transaction.addRecord("Courses", values);
    }

    List<String> enrollmentLines = Files.readAllLines(Paths.get("enrollments.csv"), Charset.defaultCharset());

    for (String line : enrollmentLines) {
      String[] splits = line.split(",");
      List<DataType> values = new ArrayList<DataType>();

      values.add(new IntDataType(Integer.parseInt(splits[0])));
      values.add(new IntDataType(Integer.parseInt(splits[1])));

      transaction.addRecord("Enrollments", values);
    }

    transaction.end();
  }
}
