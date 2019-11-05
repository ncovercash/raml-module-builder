package org.folio.cql2pgjson;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.folio.cql2pgjson.exception.QueryValidationException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;

@RunWith(JUnitParamsRunner.class)
public class ForeignKeyGenerationIT extends DatabaseTestBase {
  private CQL2PgJSON cql2pgJsonTablea;
  private CQL2PgJSON cql2pgJsonTableb;
  private CQL2PgJSON cql2pgJsonTablec;

  @BeforeClass
  public static void runOnceBeforeClass() throws Exception {
    setupDatabase();
    runSqlFile("foreignKey.sql");
  }

  @AfterClass
  public static void runOnceAfterClass() {
    closeDatabase();
  }

  @Before
  public void before() throws Exception {
    cql2pgJsonTablea = new CQL2PgJSON("tablea.jsonb");
    cql2pgJsonTablea.setDbSchemaPath("templates/db_scripts/foreignKey.json");
    cql2pgJsonTableb = new CQL2PgJSON("tableb.jsonb");
    cql2pgJsonTableb.setDbSchemaPath("templates/db_scripts/foreignKey.json");
    cql2pgJsonTablec = new CQL2PgJSON("tablec.jsonb");
    cql2pgJsonTablec.setDbSchemaPath("templates/db_scripts/foreignKey.json");
  }

  private List<String> cql(String cql) {
    try {
      String sql = cql2pgJsonTablea.toSql(cql).toString();
      return firstColumn("select jsonb->>'name' from tablea " + sql);
    } catch (QueryValidationException e) {
      throw new RuntimeException(e);
    }
  }
  private List<String> cqlb(String cql) {
    try {
      String sql = cql2pgJsonTableb.toSql(cql).toString();
      return firstColumn("select jsonb->>'prefix' from tableb " + sql);
    } catch (QueryValidationException e) {
      throw new RuntimeException(e);
    }
  }
  private List<String> cqlc(String cql) {
    try {
      String sql = cql2pgJsonTablec.toSql(cql).toString();
      return firstColumn("select jsonb->>'cindex' from tablec " + sql);
    } catch (QueryValidationException e) {
      throw new RuntimeException(e);
    }
  }
  @Test
  public void foreignKeySearch0() throws Exception {
    assertThat(cql("tableb.prefix == x0"), is(empty()));
  }

  @Test
  public void foreignKeySearchParentChild() throws Exception {
    assertThat(cqlb("tablea.name == test1"), containsInAnyOrder("x1"));
  }

  @Test
  public void foreignKeySearchMulti() throws Exception {
    assertThat(cql("tablec.cindex == z1"), containsInAnyOrder("test1"));
  }

  @Test
  public void foreignKeySearchMulti2() throws Exception {
    assertThat(cql("tablec.cindex == z2"), containsInAnyOrder("test2"));
  }

  @Test
  public void foreignKeySearchMultiWild() throws Exception {
    assertThat(cql("tablec.cindex == *"), containsInAnyOrder("test1", "test2"));
  }

  @Test
  public void foreignKeySearchMultiChildParent() throws Exception {
    assertThat(cqlc("tablea.name == test3"), is(empty()));
  }

  @Test
  public void foreignKeySearchMultiWildChildParent() throws Exception {
    assertThat(cqlc("tablea.name == *"), containsInAnyOrder("z1", "z2", "z3", "z4"));
  }

  @Test
  public void foreignKeySearch1() throws Exception {
    assertThat(cql("tableb.prefix == x1"), containsInAnyOrder("test1"));
  }

  @Test
  public void foreignKeySearch2() throws Exception {
    // two tableb records match, but they both reference the same tablea record, therefore "test2" should be
    // returned one time only
    assertThat(cql("tableb.prefix == x2"), containsInAnyOrder("test2"));
  }

  @Test
  public void foreignKeySearchWild() throws Exception {
    assertThat(cql("tableb.prefix == *"), containsInAnyOrder("test1", "test2", "test3"));
  }

  @Test
  public void foreignKeySearchWild2() throws Exception {
    assertThat(cql("tableb.prefix = *"), containsInAnyOrder("test1", "test2", "test3"));
  }

  @Test
  public void uuidConstant() throws Exception {
    assertThat(cql("tableb.name == 33333333-3333-3333-3333-333333333333"), is(empty()));
  }

  @Test
  public void ForeignKeySearchWithInjection1() {
    //check to see if we can execute some arbitrary sql
    assertThat(cql("tableb.prefix == \"x0')));((('DROP tableb\""), containsInAnyOrder("test3") );
    //then check to see if the drop table worked by checking to see if there is anything there
    assertThat(cql("tableb.prefix == \"x0')));((('DROP tableb\"").size() > 0, is(true));
  }

  @Test
  public void ForeignKeySearchWithInjection2() {
    //this test is to see if I can query more items then intended
    //if the test passes it means it was not successful
    assertThat(cql("tableb.prefix == \"x0')  or 1=1)--\""), is(empty()) );
  }
}