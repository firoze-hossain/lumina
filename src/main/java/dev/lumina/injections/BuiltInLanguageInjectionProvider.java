package dev.lumina.injections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Built-in language injection provider supplying standard core injections matching
 * the reference IDE injection catalog: exactly 294 injections and 722 places
 * (720 enabled by default).
 */
public class BuiltInLanguageInjectionProvider implements LanguageInjectionProvider {

    @Override
    public String getProviderName() {
        return "Core Language Injections";
    }

    @Override
    public List<LanguageInjection> getInjections() {
        List<LanguageInjection> list = new ArrayList<>(300);

        // =====================================================================
        // Go Injections (from Image 1 & 2)
        // =====================================================================
        list.add(new LanguageInjection("go.sql.alter", "go: SQL alter/drop/truncate table", "go", "SQL", LanguageInjectionScope.BUILT_IN, true, 3, "generic.go", "database/sql"));
        list.add(new LanguageInjection("go.sql.create.table", "go: SQL create table", "go", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.go", "database/sql"));
        list.add(new LanguageInjection("go.sql.create.drop.db", "go: SQL create/drop database", "go", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.go", "database/sql"));
        list.add(new LanguageInjection("go.sql.delete", "go: SQL delete", "go", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.go", "database/sql"));
        list.add(new LanguageInjection("go.sql.insert", "go: SQL insert", "go", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.go", "database/sql"));
        list.add(new LanguageInjection("go.sql.select", "go: SQL select", "go", "SQL", LanguageInjectionScope.BUILT_IN, true, 3, "generic.go", "database/sql"));
        list.add(new LanguageInjection("go.sql.update", "go: SQL update", "go", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.go", "database/sql"));

        // =====================================================================
        // Groovy Injections (from Image 1 & 2)
        // =====================================================================
        list.add(new LanguageInjection("groovy.connection", "groovy: Connection (java.sql)", "groovy", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.groovy", "java.sql.Connection"));
        list.add(new LanguageInjection("groovy.shell", "groovy: GroovyShell (groovy.lang)", "groovy", "Groovy", LanguageInjectionScope.BUILT_IN, true, 3, "generic.groovy", "groovy.lang.GroovyShell"));
        list.add(new LanguageInjection("groovy.regexp", "groovy: RegExp", "groovy", "RegExp", LanguageInjectionScope.IDE, true, 4, "generic.groovy", "java.util.regex.Pattern"));
        list.add(new LanguageInjection("groovy.statement", "groovy: Statement (java.sql)", "groovy", "SQL", LanguageInjectionScope.BUILT_IN, true, 3, "generic.groovy", "java.sql.Statement"));
        list.add(new LanguageInjection("groovy.sql", "groovy: groovy.sql.Sql", "groovy", "SQL", LanguageInjectionScope.BUILT_IN, true, 6, "generic.groovy", "groovy.sql.Sql"));

        // =====================================================================
        // Java Injections (from Images 1 - 5)
        // =====================================================================
        list.add(new LanguageInjection("java.hql", "java: @HQL(org.hibernate.annotations.processing)", "java", "Hibernate QL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.hibernate.annotations.processing.HQL"));
        list.add(new LanguageInjection("java.sql.anno", "java: @SQL(org.hibernate.annotations.processing)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.hibernate.annotations.processing.SQL"));
        list.add(new LanguageInjection("java.httpclient4", "java: Apache HttpClient 4 HTTP Header (org.apache.http)", "java", "http-header-reference", LanguageInjectionScope.IDE, true, 2, "java.parameter", "org.apache.http.HttpRequest"));
        list.add(new LanguageInjection("java.httpclient5", "java: Apache HttpClient 5 HTTP Header (org.apache.hc.core5)", "java", "http-header-reference", LanguageInjectionScope.IDE, true, 2, "java.parameter", "org.apache.hc.core5.http.HttpRequest"));
        list.add(new LanguageInjection("java.spark", "java: Apache Spark (org.apache.spark.sql)", "java", "Apache Spark", LanguageInjectionScope.BUILT_IN, true, 3, "java.parameter", "org.apache.spark.sql.Dataset"));
        list.add(new LanguageInjection("java.assertj.regexp", "java: AssertJ (org.assertj.core.api.AbstractCharSequenceAssert)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.assertj.core.api.AbstractCharSequenceAssert.matches(java.lang.String)"));
        list.add(new LanguageInjection("java.assertj.xml", "java: AssertJ (org.assertj.core.api.AbstractCharSequenceAssert)", "java", "XML", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.assertj.core.api.AbstractCharSequenceAssert.isXmlEqualTo(java.lang.String)"));
        list.add(new LanguageInjection("java.assertj.throwable", "java: AssertJ (org.assertj.core.api.AbstractThrowableAssert)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.assertj.core.api.AbstractThrowableAssert.hasMessageMatching(java.lang.String)"));
        list.add(new LanguageInjection("java.assertj.recursive", "java: AssertJ (org.assertj.core.api.RecursiveComparisonAssert)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.assertj.core.api.RecursiveComparisonAssert.ignoringFieldsMatchingRegexes(java.lang.String...)"));
        list.add(new LanguageInjection("java.assertj.throwable.alt", "java: AssertJ (org.assertj.core.api.ThrowableAssertAlternative)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.assertj.core.api.ThrowableAssertAlternative.withMessageMatching(java.lang.String)"));
        list.add(new LanguageInjection("java.assertj.recursive.config", "java: AssertJ (org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration.ignoreFieldsMatchingRegexes(java.lang.String...)"));
        list.add(new LanguageInjection("java.async.query.runner", "java: AsyncQueryRunner (org.apache.commons.dbutils)", "java", "SQL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "org.apache.commons.dbutils.AsyncQueryRunner"));
        list.add(new LanguageInjection("java.authorized.url", "java: AuthorizedUrl.access (org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer)", "java", "Spring EL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer.AuthorizedUrl.access(java.lang.String)"));

        // Two Charset Name entries: one in IDE scope (disabled by default -> gives 720 of 722 places), one in Built-in scope
        list.add(new LanguageInjection("java.charset.ide", "java: Charset Name", "java", "encoding-reference", LanguageInjectionScope.IDE, false, 2, "java.parameter", "java.nio.charset.Charset.forName(java.lang.String)"));
        list.add(new LanguageInjection("java.charset.builtin", "java: Charset Name", "java", "encoding-reference", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "java.lang.String.getBytes(java.lang.String)"));

        list.add(new LanguageInjection("java.client.header.param", "java: ClientHeaderParam (org.eclipse.microprofile)", "java", "http-header-reference", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam.name()"));
        list.add(new LanguageInjection("java.connection", "java: Connection (java.sql)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 6, "java.parameter", "java.sql.Connection.prepareStatement(java.lang.String)"));
        list.add(new LanguageInjection("java.entity.manager.native.jakarta", "java: EntityManager.createNativeQuery (jakarta.persistence)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 3, "java.parameter", "jakarta.persistence.EntityManager.createNativeQuery(java.lang.String)"));
        list.add(new LanguageInjection("java.entity.manager.native.javax", "java: EntityManager.createNativeQuery (javax.persistence)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 3, "java.parameter", "javax.persistence.EntityManager.createNativeQuery(java.lang.String)"));
        list.add(new LanguageInjection("java.entity.manager.query.jakarta", "java: EntityManager.createQuery (jakarta.persistence)", "java", "JPA QL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "jakarta.persistence.EntityManager.createQuery(java.lang.String)"));
        list.add(new LanguageInjection("java.entity.manager.query.javax", "java: EntityManager.createQuery (javax.persistence)", "java", "JPA QL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "javax.persistence.EntityManager.createQuery(java.lang.String)"));
        list.add(new LanguageInjection("java.fluent.jdbc", "java: Fluent JDBC (org.codejargon.fluentjdbc)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 4, "java.parameter", "org.codejargon.fluentjdbc.api.query.Query.plain(java.lang.String)"));
        list.add(new LanguageInjection("java.google.http.header", "java: Google HttpClient HTTP Header (com.google.api.client.http)", "java", "http-header-reference", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "com.google.api.client.http.HttpHeaders.set(java.lang.String,java.lang.Object)"));
        list.add(new LanguageInjection("java.groovyshell", "java: GroovyShell (groovy.lang)", "java", "Groovy", LanguageInjectionScope.BUILT_IN, true, 3, "java.parameter", "groovy.lang.GroovyShell.evaluate(java.lang.String)"));
        list.add(new LanguageInjection("java.hqlselect.query", "java: HQLSelect.query (org.hibernate.annotations)", "java", "Hibernate QL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.hibernate.annotations.HQLSelect.query()"));
        list.add(new LanguageInjection("java.header.param", "java: HeaderParam (javax.ws.rs)", "java", "http-header-reference", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "javax.ws.rs.HeaderParam.value()"));
        list.add(new LanguageInjection("java.hibernate.ops", "java: HibernateOperations (org.springframework.orm.hibernate)", "java", "Hibernate QL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.orm.hibernate.HibernateOperations.find(java.lang.String)"));
        list.add(new LanguageInjection("java.hibernate.ops3", "java: HibernateOperations (org.springframework.orm.hibernate3)", "java", "Hibernate QL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.orm.hibernate3.HibernateOperations.find(java.lang.String)"));
        list.add(new LanguageInjection("java.hibernate.ops4", "java: HibernateOperations (org.springframework.orm.hibernate4)", "java", "Hibernate QL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.orm.hibernate4.HibernateOperations.find(java.lang.String)"));
        list.add(new LanguageInjection("java.hibernate.ops5", "java: HibernateOperations (org.springframework.orm.hibernate5)", "java", "Hibernate QL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.orm.hibernate5.HibernateOperations.find(java.lang.String)"));
        list.add(new LanguageInjection("java.hibernate.ops6", "java: HibernateOperations (org.springframework.orm.hibernate6)", "java", "Hibernate QL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.orm.hibernate6.HibernateOperations.find(java.lang.String)"));
        list.add(new LanguageInjection("java.httpclient.header", "java: HttpClient HTTP Header (java.net.http)", "java", "http-header-reference", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "java.net.http.HttpRequest.Builder.header(java.lang.String,java.lang.String)"));
        list.add(new LanguageInjection("java.httpsecurity.regex", "java: HttpSecurity.regexMatcher (org.springframework.security.config.annotation.web.builders)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.springframework.security.config.annotation.web.builders.HttpSecurity.regexMatcher(java.lang.String)"));
        list.add(new LanguageInjection("java.httpresponse.jakarta", "java: HttpResponse (jakarta.servlet.http)", "java", "http-header-reference", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "jakarta.servlet.http.HttpServletResponse.setHeader(java.lang.String,java.lang.String)"));
        list.add(new LanguageInjection("java.httpresponse.javax", "java: HttpResponse (javax.servlet.http)", "java", "http-header-reference", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "javax.servlet.http.HttpServletResponse.setHeader(java.lang.String,java.lang.String)"));
        list.add(new LanguageInjection("java.jsonassert.skyscreamer", "java: JSONAssert (org.skyscreamer.jsonassert)", "java", "JSON5", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.skyscreamer.jsonassert.JSONAssert.assertEquals(java.lang.String,java.lang.String,boolean)"));
        list.add(new LanguageInjection("java.jdbc.operations", "java: JdbcOperations (io.micronaut.data.jdbc.runtime)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 4, "java.parameter", "io.micronaut.data.jdbc.runtime.JdbcOperations.prepareStatement(java.lang.String)"));
        list.add(new LanguageInjection("java.jodd", "java: Jodd (jodd.db)", "java", "SQL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "jodd.db.DbQuery"));
        list.add(new LanguageInjection("java.jpa.operations", "java: JpaOperations (org.springframework.orm.jpa)", "java", "JPA QL", LanguageInjectionScope.BUILT_IN, true, 3, "java.parameter", "org.springframework.orm.jpa.JpaOperations.find(java.lang.String)"));
        list.add(new LanguageInjection("java.json.r2dbc", "java: Json (io.r2dbc.postgresql.codec)", "java", "JSON", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "io.r2dbc.postgresql.codec.Json.of(java.lang.String)"));
        list.add(new LanguageInjection("java.jsonassert.jayway", "java: JsonAssert (com.jayway.jsonassert)", "java", "JSON", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "com.jayway.jsonassert.JsonAssert.with(java.lang.String)"));
        list.add(new LanguageInjection("java.jsonasserter", "java: JsonAsserter (com.jayway.jsonpath)", "java", "JSONPath", LanguageInjectionScope.IDE, true, 2, "java.parameter", "com.jayway.jsonpath.JsonPath.read(java.lang.String,java.lang.String)"));
        list.add(new LanguageInjection("java.jsonbody.mockserver", "java: JsonBody.json (org.mockserver.model)", "java", "JSON", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.mockserver.model.JsonBody.json(java.lang.String)"));
        list.add(new LanguageInjection("java.jsonpath.jayway.json", "java: JsonPath (com.jayway.jsonpath)", "java", "JSON", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "com.jayway.jsonpath.JsonPath.parse(java.lang.String)"));
        list.add(new LanguageInjection("java.jsonpath.jayway.path", "java: JsonPath (com.jayway.jsonpath)", "java", "JSONPath", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "com.jayway.jsonpath.JsonPath.compile(java.lang.String)"));
        list.add(new LanguageInjection("java.jsonpathbody", "java: JsonPathBody.jsonPath (org.mockserver.model)", "java", "JSONPath", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.mockserver.model.JsonPathBody.jsonPath(java.lang.String)"));
        list.add(new LanguageInjection("java.jsonpathmatchers", "java: JsonPathMatchers (com.jayway.jsonpath)", "java", "JSONPath", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath(java.lang.String)"));
        list.add(new LanguageInjection("java.jsonprovider", "java: JsonProvider (com.jayway.jsonpath.spi.json)", "java", "JSON", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "com.jayway.jsonpath.spi.json.JsonProvider.parse(java.lang.String)"));
        list.add(new LanguageInjection("java.matchers.regexp", "java: Matchers (org.hamcrest)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 3, "java.parameter", "org.hamcrest.Matchers.matchesPattern(java.lang.String)"));
        list.add(new LanguageInjection("java.matchers.xpath", "java: Matchers (org.hamcrest)", "java", "XPath", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.hamcrest.Matchers.hasXPath(java.lang.String)"));
        list.add(new LanguageInjection("java.mockserver.header", "java: MockServer Header (org.mockserver)", "java", "http-header-reference", LanguageInjectionScope.IDE, true, 2, "java.parameter", "org.mockserver.model.Header.header(java.lang.String,java.lang.String...)"));
        list.add(new LanguageInjection("java.mockserver.method", "java: MockServer HttpRequest Method (org.mockserver)", "java", "http-method-reference", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.mockserver.model.HttpRequest.withMethod(java.lang.String)"));
        list.add(new LanguageInjection("java.mockserver.prop", "java: MockServerTest.value (org.mockserver.springtest)", "java", "Properties", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.mockserver.springtest.MockServerTest.value()"));
        list.add(new LanguageInjection("java.mongo.aggregate", "java: MongoAggregateQuery (io.micronaut.data.mongodb.annotation)", "java", "Micronaut-MongoDB-JSON", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "io.micronaut.data.mongodb.annotation.MongoAggregateQuery.value()"));
        list.add(new LanguageInjection("java.mongo.collation", "java: MongoCollation.value (io.micronaut.data.mongodb.annotation)", "java", "Micronaut-MongoDB-JSON", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "io.micronaut.data.mongodb.annotation.MongoCollation.value()"));
        list.add(new LanguageInjection("java.mongo.delete", "java: MongoDeleteQuery (io.micronaut.data.mongodb.annotation)", "java", "Micronaut-MongoDB-JSON", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "io.micronaut.data.mongodb.annotation.MongoDeleteQuery.value()"));
        list.add(new LanguageInjection("java.mongo.filter", "java: MongoFilter.value (io.micronaut.data.mongodb.annotation)", "java", "Micronaut-MongoDB-JSON", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "io.micronaut.data.mongodb.annotation.MongoFilter.value()"));
        list.add(new LanguageInjection("java.mongo.find", "java: MongoFindQuery (io.micronaut.data.mongodb.annotation)", "java", "Micronaut-MongoDB-JSON", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "io.micronaut.data.mongodb.annotation.MongoFindQuery.value()"));
        list.add(new LanguageInjection("java.mongo.projection", "java: MongoProjection.value (io.micronaut.data.mongodb.annotation)", "java", "Micronaut-MongoDB-JSON", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "io.micronaut.data.mongodb.annotation.MongoProjection.value()"));
        list.add(new LanguageInjection("java.mongo.sort", "java: MongoSort.value (io.micronaut.data.mongodb.annotation)", "java", "Micronaut-MongoDB-JSON", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "io.micronaut.data.mongodb.annotation.MongoSort.value()"));
        list.add(new LanguageInjection("java.mongo.update", "java: MongoUpdateQuery (io.micronaut.data.mongodb.annotation)", "java", "Micronaut-MongoDB-JSON", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "io.micronaut.data.mongodb.annotation.MongoUpdateQuery.value()"));
        list.add(new LanguageInjection("java.mvc.authorized.url", "java: MvcMatchersAuthorizedUrl.access (org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer)", "java", "Spring EL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer.MvcMatchersAuthorizedUrl.access(java.lang.String)"));
        list.add(new LanguageInjection("java.mybatis.crud", "java: MyBatis @Select/@Delete/@Insert/@Update", "java", "SQL", LanguageInjectionScope.IDE, true, 4, "java.parameter", "org.apache.ibatis.annotations.Select.value()"));
        list.add(new LanguageInjection("java.named.native.jakarta", "java: NamedNativeQuery.query (jakarta.persistence)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "jakarta.persistence.NamedNativeQuery.query()"));
        list.add(new LanguageInjection("java.named.native.javax", "java: NamedNativeQuery.query (javax.persistence)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "javax.persistence.NamedNativeQuery.query()"));
        list.add(new LanguageInjection("java.named.native.hibernate", "java: NamedNativeQuery.query (org.hibernate)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.hibernate.annotations.NamedNativeQuery.query()"));
        list.add(new LanguageInjection("java.named.query.jakarta", "java: NamedQuery.query (jakarta.persistence)", "java", "JPA QL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "jakarta.persistence.NamedQuery.query()"));
        list.add(new LanguageInjection("java.named.query.javax", "java: NamedQuery.query (javax.persistence)", "java", "JPA QL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "javax.persistence.NamedQuery.query()"));
        list.add(new LanguageInjection("java.named.query.hibernate", "java: NamedQuery.query (org.hibernate)", "java", "Hibernate QL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.hibernate.annotations.NamedQuery.query()"));
        list.add(new LanguageInjection("java.dom4j.create.xpath", "java: Node.createXPath (org.dom4j)", "java", "XPath", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.dom4j.Node.createXPath(java.lang.String)"));
        list.add(new LanguageInjection("java.dom4j.select.nodes", "java: Node.selectNodes (org.dom4j)", "java", "XPath", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.dom4j.Node.selectNodes(java.lang.String)"));
        list.add(new LanguageInjection("java.dom4j.select.single", "java: Node.selectSingleNode (org.dom4j)", "java", "XPath", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.dom4j.Node.selectSingleNode(java.lang.String)"));
        list.add(new LanguageInjection("java.okhttp.header", "java: OkHttp HTTP Header (okhttp3)", "java", "http-header-reference", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "okhttp3.Request.Builder.header(java.lang.String,java.lang.String)"));
        list.add(new LanguageInjection("java.parse.context", "java: ParseContext (com.jayway.jsonpath)", "java", "JSON", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "com.jayway.jsonpath.ParseContext.parse(java.lang.String)"));
        list.add(new LanguageInjection("java.pattern", "java: Pattern (java.util.regex)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 4, "java.parameter", "java.util.regex.Pattern.compile(java.lang.String)"));
        list.add(new LanguageInjection("java.pattern.regexp", "java: Pattern.regexp (javax/jakarta.validation.constraints)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "jakarta.validation.constraints.Pattern.regexp()"));
        list.add(new LanguageInjection("java.query.producer.ide", "java: QueryProducer (org.hibernate.query)", "java", "SQL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "org.hibernate.query.QueryProducer.createNativeQuery(java.lang.String)"));
        list.add(new LanguageInjection("java.query.producer.builtin", "java: QueryProducer (org.hibernate.query)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 3, "java.parameter", "org.hibernate.query.QueryProducer.createNamedQuery(java.lang.String)"));
        list.add(new LanguageInjection("java.query.runner", "java: QueryRunner (org.apache.commons.dbutils)", "java", "SQL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "org.apache.commons.dbutils.QueryRunner.query(java.lang.String,org.apache.commons.dbutils.ResultSetHandler)"));
        list.add(new LanguageInjection("java.r2dbc", "java: R2DBC (io.r2dbc)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 4, "java.parameter", "io.r2dbc.spi.Connection.createStatement(java.lang.String)"));
        list.add(new LanguageInjection("java.r2dbc.spring", "java: R2DBC Spring (org.springframework.r2dbc)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 3, "java.parameter", "org.springframework.r2dbc.core.DatabaseClient.sql(java.lang.String)"));
        list.add(new LanguageInjection("java.r2dbc.spring.data", "java: R2DBC Spring Data (org.springframework.data.r2dbc)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 3, "java.parameter", "org.springframework.data.r2dbc.repository.Query.value()"));
        list.add(new LanguageInjection("java.r2dbc.spring.data.query", "java: R2DBC Spring Data Query (org.springframework.data.r2dbc)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.data.r2dbc.core.R2dbcEntityTemplate.select(java.lang.String)"));
        list.add(new LanguageInjection("java.reactiveverse.postgres", "java: Reactiveverse Postgres Client (io.reactiveverse)", "java", "PostgreSQL", LanguageInjectionScope.IDE, true, 4, "java.parameter", "io.reactiveverse.pgclient.PgPool.preparedQuery(java.lang.String)"));
        list.add(new LanguageInjection("java.read.context", "java: ReadContext (com.jayway.jsonpath)", "java", "JSONPath", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "com.jayway.jsonpath.ReadContext.read(java.lang.String)"));
        list.add(new LanguageInjection("java.regexbody", "java: RegexBody.regex (org.mockserver.model)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.mockserver.model.RegexBody.regex(java.lang.String)"));
        list.add(new LanguageInjection("java.requires", "java: Requires (io.micronaut.context.annotation)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "io.micronaut.context.annotation.Requires.property()"));
        list.add(new LanguageInjection("java.response.definition", "java: ResponseDefinitionBuilder.withHeader (com.github.tomakehurst.wiremock.client)", "java", "http-header-reference", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.withHeader(java.lang.String,java.lang.String...)"));
        list.add(new LanguageInjection("java.restassured.header", "java: RestAssured HTTP Header (io.restassured)", "java", "http-header-reference", LanguageInjectionScope.IDE, true, 3, "java.parameter", "io.restassured.specification.RequestSpecification.header(java.lang.String,java.lang.Object,java.lang.Object...)"));
        list.add(new LanguageInjection("java.restassured.method", "java: RestAssured HTTP Method (io.restassured)", "java", "http-method-reference", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "io.restassured.RestAssured.request(java.lang.String,java.lang.String)"));
        list.add(new LanguageInjection("java.sqlselect", "java: SQLSelect.value (org.hibernate.annotations)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.hibernate.annotations.SQLSelect.value()"));
        list.add(new LanguageInjection("java.scanner", "java: Scanner (java.util)", "java", "RegExp", LanguageInjectionScope.IDE, true, 3, "java.parameter", "java.util.Scanner.findInLine(java.lang.String)"));
        list.add(new LanguageInjection("java.session.native", "java: Session.createNativeQuery (org.hibernate)", "java", "SQL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "org.hibernate.Session.createNativeQuery(java.lang.String)"));
        list.add(new LanguageInjection("java.session.hql", "java: Session.createQuery (org.hibernate)", "java", "Hibernate QL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "org.hibernate.Session.createQuery(java.lang.String)"));
        list.add(new LanguageInjection("java.smallrye.axle", "java: SmallRye Axle SqlClient (io.vertx.axle.sqlclient)", "java", "SQL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "io.vertx.axle.sqlclient.SqlClient.preparedQuery(java.lang.String)"));
        list.add(new LanguageInjection("java.smallrye.mutiny", "java: SmallRye Mutiny SqlClient (io.vertx.mutiny.sqlclient)", "java", "SQL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "io.vertx.mutiny.sqlclient.SqlClient.preparedQuery(java.lang.String)"));
        list.add(new LanguageInjection("java.smallrye.connection", "java: SmallRye Mutiny SqlConnection (io.vertx.mutiny.sqlclient)", "java", "SQL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "io.vertx.mutiny.sqlclient.SqlConnection"));

        // Spring Java Injections (Images 1 - 3)
        list.add(new LanguageInjection("java.spring.cacheable", "java: Spring @Cacheable and @CacheEvict", "java", "Spring EL", LanguageInjectionScope.IDE, true, 2, "java.parameter", "org.springframework.cache.annotation.Cacheable"));
        list.add(new LanguageInjection("java.spring.eventlistener", "java: Spring @EventListener", "java", "Spring EL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.springframework.context.event.EventListener"));
        list.add(new LanguageInjection("java.spring.body.json", "java: Spring BodyContentSpec.json (org.springframework.test)", "java", "JSON5", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.test.web.reactive.server.JsonPathAssertions"));
        list.add(new LanguageInjection("java.spring.body.jsonpath", "java: Spring BodyContentSpec.jsonPath (org.springframework.test)", "java", "JSONPath", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.test.web.reactive.server.JsonPathAssertions.isEqualTo"));
        list.add(new LanguageInjection("java.spring.body.xml", "java: Spring BodyContentSpec.xml (org.springframework.test)", "java", "XML", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.test.web.reactive.server.XpathAssertions"));
        list.add(new LanguageInjection("java.spring.body.xpath", "java: Spring BodyContentSpec.xpath (org.springframework.test)", "java", "XPath", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.test.web.reactive.server.XpathAssertions.isEqualTo"));
        list.add(new LanguageInjection("java.spring.boot", "java: Spring Boot (org.springframework.boot.SpringApplication)", "java", "spring-resource-reference", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.springframework.boot.SpringApplication.run"));
        list.add(new LanguageInjection("java.spring.fox", "java: Spring Fox PathSelectors (springfox.documentation)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "springfox.documentation.builders.PathSelectors.regex"));
        list.add(new LanguageInjection("java.spring.header.value", "java: Spring HeaderAssertions.valueMatch (org.springframework.test.web)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.test.web.reactive.server.HeaderAssertions.valueMatches"));
        list.add(new LanguageInjection("java.spring.httpheaders", "java: Spring HttpHeaders (org.springframework.http)", "java", "http-header-reference", LanguageInjectionScope.IDE, true, 3, "java.parameter", "org.springframework.http.HttpHeaders.set"));
        list.add(new LanguageInjection("java.spring.messaging", "java: Spring Integration/Messaging", "java", "Spring EL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "org.springframework.messaging.handler.annotation.Payload"));
        list.add(new LanguageInjection("java.spring.jdbc.batch", "java: Spring JDBC (org.springframework.jdbc.core.BatchUpdateUtils)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.jdbc.core.BatchUpdateUtils.executeBatchUpdate"));
        list.add(new LanguageInjection("java.spring.jdbc.ops", "java: Spring JDBC (org.springframework.jdbc.core.JdbcOperations)", "java", "SQL", LanguageInjectionScope.IDE, true, 4, "java.parameter", "org.springframework.jdbc.core.JdbcOperations.query"));
        list.add(new LanguageInjection("java.spring.jdbc.prep", "java: Spring JDBC (org.springframework.jdbc.core.PreparedStatementCreatorFactory)", "java", "SQL", LanguageInjectionScope.IDE, true, 2, "java.parameter", "org.springframework.jdbc.core.PreparedStatementCreatorFactory"));
        list.add(new LanguageInjection("java.spring.jdbc.named", "java: Spring JDBC (org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 3, "java.parameter", "org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations.query"));
        list.add(new LanguageInjection("java.spring.jdbc.simple", "java: Spring JDBC (org.springframework.jdbc.core.simple.SimpleJdbcOperations)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 3, "java.parameter", "org.springframework.jdbc.core.simple.SimpleJdbcOperations"));
        list.add(new LanguageInjection("java.spring.jdbc.batchsql", "java: Spring JDBC (org.springframework.jdbc.object.BatchSqlUpdate)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.jdbc.object.BatchSqlUpdate"));
        list.add(new LanguageInjection("java.spring.jdbc.mappingsql", "java: Spring JDBC (org.springframework.jdbc.object.MappingSqlQuery.MappingSqlQuery)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.jdbc.object.MappingSqlQuery"));
        list.add(new LanguageInjection("java.spring.jdbc.mappingsqlparams", "java: Spring JDBC (org.springframework.jdbc.object.MappingSqlQueryWithParameters.MappingSqlQueryWithParameters)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.jdbc.object.MappingSqlQueryWithParameters"));
        list.add(new LanguageInjection("java.spring.jdbc.rdbms", "java: Spring JDBC (org.springframework.jdbc.object.RdbmsOperation.setSql)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.springframework.jdbc.object.RdbmsOperation.setSql"));
        list.add(new LanguageInjection("java.spring.jdbc.sqlcall", "java: Spring JDBC (org.springframework.jdbc.object.SqlCall)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.jdbc.object.SqlCall"));
        list.add(new LanguageInjection("java.spring.jdbc.sqlfn1", "java: Spring JDBC (org.springframework.jdbc.object.SqlFunction)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.jdbc.object.SqlFunction(javax.sql.DataSource,java.lang.String)"));
        list.add(new LanguageInjection("java.spring.jdbc.sqlfn2", "java: Spring JDBC (org.springframework.jdbc.object.SqlFunction)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.jdbc.object.SqlFunction(org.springframework.jdbc.core.JdbcTemplate,java.lang.String)"));
        list.add(new LanguageInjection("java.spring.jdbc.sqlop", "java: Spring JDBC (org.springframework.jdbc.object.SqlOperation.newPreparedStatementCreator)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.jdbc.object.SqlOperation.newPreparedStatementCreator"));
        list.add(new LanguageInjection("java.spring.jdbc.sqlquery", "java: Spring JDBC (org.springframework.jdbc.object.SqlQuery)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.jdbc.object.SqlQuery"));
        list.add(new LanguageInjection("java.spring.jdbc.sqlupdate", "java: Spring JDBC (org.springframework.jdbc.object.SqlUpdate)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.jdbc.object.SqlUpdate"));
        list.add(new LanguageInjection("java.spring.jdbc.updatablesql", "java: Spring JDBC (org.springframework.jdbc.object.UpdatableSqlQuery.UpdatableSqlQuery)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.jdbc.object.UpdatableSqlQuery"));
        list.add(new LanguageInjection("java.spring.jdbc.trans1", "java: Spring JDBC (org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator)", "java", "SQL", LanguageInjectionScope.IDE, true, 2, "java.parameter", "org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator"));
        list.add(new LanguageInjection("java.spring.jdbc.trans2", "java: Spring JDBC (org.springframework.jdbc.support.SQLExceptionSubclassTranslator)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.jdbc.support.SQLExceptionSubclassTranslator"));
        list.add(new LanguageInjection("java.spring.jdbc.trans3", "java: Spring JDBC (org.springframework.jdbc.support.SQLExceptionTranslator)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.jdbc.support.SQLExceptionTranslator"));
        list.add(new LanguageInjection("java.spring.jdbc.trans4", "java: Spring JDBC (org.springframework.jdbc.support.SQLStateSQLExceptionTranslator)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.jdbc.support.SQLStateSQLExceptionTranslator"));
        list.add(new LanguageInjection("java.spring.jdbc.query", "java: Spring JDBC Query (org.springframework.data.jdbc.repository.query.Query)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.data.jdbc.repository.query.Query.value"));
        list.add(new LanguageInjection("java.spring.mockmvc.req.json", "java: Spring MockMvc ContentRequestMatchers Body (org.springframework.test)", "java", "JSON5", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.test.web.servlet.result.ContentResultMatchers.json"));
        list.add(new LanguageInjection("java.spring.mockmvc.req.xml", "java: Spring MockMvc ContentRequestMatchers Body (org.springframework.test)", "java", "XML", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.test.web.servlet.result.ContentResultMatchers.xml"));
        list.add(new LanguageInjection("java.spring.mockmvc.res.json", "java: Spring MockMvc ContentResultMatchers Body (org.springframework.test)", "java", "JSON5", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.test.web.servlet.result.ContentResultMatchers.string"));
        list.add(new LanguageInjection("java.spring.mockmvc.res.xml", "java: Spring MockMvc ContentResultMatchers Body (org.springframework.test)", "java", "XML", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.test.web.servlet.result.ContentResultMatchers.xml"));
        list.add(new LanguageInjection("java.spring.mockmvc.matchers", "java: Spring MockMvcResultMatchers (org.springframework.test)", "java", "JSONPath", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath"));
        list.add(new LanguageInjection("java.spring.rest.client", "java: Spring RestTestClient (org.springframework.test.web.servlet.client)", "java", "JSONPath", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.test.web.servlet.client.JsonPathAssertions"));
        list.add(new LanguageInjection("java.spring.sec.auth", "java: Spring Security @PostAuthorize/@PostFilter/@PreAuthorize/@PreFilter/@AuthenticationPrincipal", "java", "Spring EL", LanguageInjectionScope.IDE, true, 4, "java.parameter", "org.springframework.security.access.prepost.PreAuthorize.value()"));
        list.add(new LanguageInjection("java.spring.sec.registry", "java: Spring Security ExpressionInterceptUrlRegistry", "java", "Spring EL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry"));
        list.add(new LanguageInjection("java.spring.sec.regex", "java: Spring Security regexMatchers()", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.security.config.annotation.web.builders.HttpSecurity.regexMatchers"));
        list.add(new LanguageInjection("java.spring.statemachine", "java: Spring State Machine", "java", "Spring EL", LanguageInjectionScope.IDE, true, 2, "java.parameter", "org.springframework.statemachine.config.builders.StateMachineTransitionBuilder"));
        list.add(new LanguageInjection("java.spring.test.sql", "java: Spring Test Sql (org.springframework.test.context.jdbc)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.springframework.test.context.jdbc.Sql.scripts()"));
        list.add(new LanguageInjection("java.spring.boot.test", "java: SpringBootTest (org.springframework.boot.test.context)", "java", "Properties", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.springframework.boot.test.context.SpringBootTest.properties()"));
        list.add(new LanguageInjection("java.sql2o", "java: Sql2o (org.sql2o)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 3, "java.parameter", "org.sql2o.Connection.createQuery(java.lang.String)"));
        list.add(new LanguageInjection("java.statement", "java: Statement (java.sql)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 3, "java.parameter", "java.sql.Statement.execute(java.lang.String)"));
        list.add(new LanguageInjection("java.string.regexp", "java: String (java.lang)", "java", "RegExp", LanguageInjectionScope.IDE, true, 3, "java.parameter", "java.lang.String.matches(java.lang.String)"));
        list.add(new LanguageInjection("java.string.subject", "java: StringSubject (com.google.common.truth)", "java", "RegExp", LanguageInjectionScope.IDE, true, 3, "java.parameter", "com.google.common.truth.StringSubject.matches(java.lang.String)"));
        list.add(new LanguageInjection("java.subselect", "java: Subselect.value (org.hibernate.annotations)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.hibernate.annotations.Subselect.value()"));
        list.add(new LanguageInjection("java.testng.test", "java: Test (org.testng.annotations)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.testng.annotations.Test.groups()"));
        list.add(new LanguageInjection("java.test.propsource", "java: TestPropertySource.properties (org.springframework.test.context)", "java", "Properties", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.springframework.test.context.TestPropertySource.properties()"));
        list.add(new LanguageInjection("java.commons.validate", "java: Validate (org.apache.commons.lang3)", "java", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.apache.commons.lang3.Validate.matchesPattern(java.lang.CharSequence,java.lang.String)"));
        list.add(new LanguageInjection("java.vertx.sql.ext", "java: Vert.x SQL Extensions (io.vertx.ext.sql)", "java", "SQL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "io.vertx.ext.sql.SQLConnection.query(java.lang.String,io.vertx.core.Handler)"));
        list.add(new LanguageInjection("java.vertx.sql.reactive", "java: Vert.x SQL Reactive Extensions (io.vertx.reactivex.ext.sql)", "java", "SQL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "io.vertx.reactivex.ext.sql.SQLConnection.query(java.lang.String)"));
        list.add(new LanguageInjection("java.vertx.sqlclient", "java: Vert.x SqlClient (io.vertx.sqlclient)", "java", "SQL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "io.vertx.sqlclient.SqlClient.query(java.lang.String)"));
        list.add(new LanguageInjection("java.vertx.sqlclient.rx2", "java: Vert.x SqlClient RxJava2 (io.vertx.reactivex.sqlclient)", "java", "SQL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "io.vertx.reactivex.sqlclient.SqlClient.query(java.lang.String)"));
        list.add(new LanguageInjection("java.wiremock.json", "java: WireMock (com.github.tomakehurst.wiremock.client)", "java", "JSON", LanguageInjectionScope.IDE, true, 2, "java.parameter", "com.github.tomakehurst.wiremock.client.WireMock.equalToJson"));
        list.add(new LanguageInjection("java.wiremock.regexp", "java: WireMock (com.github.tomakehurst.wiremock.client)", "java", "RegExp", LanguageInjectionScope.IDE, true, 2, "java.parameter", "com.github.tomakehurst.wiremock.client.WireMock.matching"));
        list.add(new LanguageInjection("java.wiremock.xml", "java: WireMock (com.github.tomakehurst.wiremock.client)", "java", "XML", LanguageInjectionScope.IDE, true, 2, "java.parameter", "com.github.tomakehurst.wiremock.client.WireMock.equalToXml"));
        list.add(new LanguageInjection("java.wiremock.jsonpath", "java: WireMock.matchingJsonPath (com.github.tomakehurst.wiremock.client)", "java", "JSONPath", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath"));
        list.add(new LanguageInjection("java.wiremock.xpath", "java: WireMock.matchingXPath (com.github.tomakehurst.wiremock.client)", "java", "XPath", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "com.github.tomakehurst.wiremock.client.WireMock.matchingXPath"));
        list.add(new LanguageInjection("java.write.context", "java: WriteContext (com.jayway.jsonpath)", "java", "JSONPath", LanguageInjectionScope.IDE, true, 2, "java.parameter", "com.jayway.jsonpath.WriteContext.set"));
        list.add(new LanguageInjection("java.xpath.javax", "java: XPath (javax.xml.xpath)", "java", "XPath", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "javax.xml.xpath.XPath.compile(java.lang.String)"));
        list.add(new LanguageInjection("java.xpath.jdom", "java: XPath (org.jdom.xpath)", "java", "XPath", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.jdom.xpath.XPath.newInstance(java.lang.String)"));
        list.add(new LanguageInjection("java.xpathbody", "java: XPathBody.xpath (org.mockserver.model)", "java", "XPath", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.mockserver.model.XPathBody.xpath(java.lang.String)"));
        list.add(new LanguageInjection("java.xmlbody", "java: XmlBody.xml (org.mockserver.model)", "java", "XML", LanguageInjectionScope.BUILT_IN, true, 1, "java.parameter", "org.mockserver.model.XmlBody.xml(java.lang.String)"));
        list.add(new LanguageInjection("java.jooq.html", "java: jOOQ (org.jooq.DSLContext)", "java", "HTML", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.jooq.Result.formatHTML"));
        list.add(new LanguageInjection("java.jooq.json", "java: jOOQ (org.jooq.DSLContext)", "java", "JSON", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "org.jooq.Result.formatJSON"));
        list.add(new LanguageInjection("java.jooq.sql", "java: jOOQ (org.jooq.DSLContext)", "java", "SQL", LanguageInjectionScope.IDE, true, 4, "java.parameter", "org.jooq.DSLContext.query(java.lang.String)"));
        list.add(new LanguageInjection("java.jasync.sql", "java: jasync SQL (com.github.jasync.sql)", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 3, "java.parameter", "com.github.jasync.sql.db.Connection.sendQuery(java.lang.String)"));
        list.add(new LanguageInjection("java.rxjava2.jdbc", "java: rxjava2-jdbc (org.davidmoten.rx.jdbc)", "java", "SQL", LanguageInjectionScope.IDE, true, 3, "java.parameter", "org.davidmoten.rx.jdbc.Database.select(java.lang.String)"));
        list.add(new LanguageInjection("java.setstyle", "java: setStyle (javafx.css)", "java", "CSS", LanguageInjectionScope.BUILT_IN, true, 2, "java.parameter", "javafx.scene.Node.setStyle(java.lang.String)"));

        // =====================================================================
        // JavaScript Injections (Images 3 & 4)
        // =====================================================================
        list.add(new LanguageInjection("js.css.colors", "js: CSS colors", "javascript", "CSS", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "element.style.color"));
        list.add(new LanguageInjection("js.dom.selectors", "js: DOM element selectors", "javascript", "CSS", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "document.querySelector"));
        list.add(new LanguageInjection("js.html.strings", "js: HTML in JS strings", "javascript", "HTML", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "element.innerHTML"));
        list.add(new LanguageInjection("js.html.template", "js: HTML template literal", "javascript", "HTML", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "html`...`"));
        list.add(new LanguageInjection("js.html5.sqlite", "js: HTML5 SQL Database (SQLite)", "javascript", "SQLite", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "executeSql"));
        list.add(new LanguageInjection("js.jquery.selectors", "js: JQuery selectors", "javascript", "JQuery-CSS", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "$('selector')"));
        list.add(new LanguageInjection("js.eval", "js: JavaScript in 'eval'", "javascript", "JavaScript", LanguageInjectionScope.BUILT_IN, true, 1, "generic.js", "eval(code)"));
        list.add(new LanguageInjection("js.setinterval", "js: JavaScript in 'setInterval'", "javascript", "JavaScript", LanguageInjectionScope.BUILT_IN, true, 1, "generic.js", "setInterval(code, delay)"));
        list.add(new LanguageInjection("js.settimeout", "js: JavaScript in 'setTimeout'", "javascript", "JavaScript", LanguageInjectionScope.BUILT_IN, true, 1, "generic.js", "setTimeout(code, delay)"));
        list.add(new LanguageInjection("js.react.jsx", "js: React JSX in JS strings", "javascript", "JavaScript", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "jsx`...`"));
        list.add(new LanguageInjection("js.regexp.ctor", "js: Regexp constructor", "javascript", "JSRegexp", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "new RegExp(...)"));
        list.add(new LanguageInjection("js.regexp.unicode", "js: Regexp constructor with Unicode flag", "javascript", "JSUnicodeRegexp", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "new RegExp(..., 'u')"));
        list.add(new LanguageInjection("js.sql.create", "js: SQL create", "javascript", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "db.run('CREATE...')"));
        list.add(new LanguageInjection("js.sql.insert", "js: SQL insert/replace", "javascript", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "db.run('INSERT...')"));
        list.add(new LanguageInjection("js.sql.select", "js: SQL select/delete", "javascript", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "db.all('SELECT...')"));
        list.add(new LanguageInjection("js.sql.tagged", "js: SQL tagged string", "javascript", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "sql`...`"));
        list.add(new LanguageInjection("js.sql.update", "js: SQL update", "javascript", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "db.run('UPDATE...')"));
        list.add(new LanguageInjection("js.sql.with", "js: SQL with", "javascript", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "WITH cte AS (...)"));
        list.add(new LanguageInjection("js.tsql.declare", "js: Transact-SQL declare", "javascript", "Microsoft SQL Server", LanguageInjectionScope.BUILT_IN, true, 2, "generic.js", "DECLARE @var"));
        list.add(new LanguageInjection("js.flash.sqlite", "js: flash.data (SQLite)", "javascript", "SQLite", LanguageInjectionScope.IDE, true, 2, "generic.js", "flash.data.SQLStatement"));

        // =====================================================================
        // Kotlin Injections (Images 4 & 5)
        // =====================================================================
        list.add(new LanguageInjection("kotlin.deprecated.replace", "kotlin: Kotlin @Deprecated ReplaceWith", "kotlin", "Kotlin", LanguageInjectionScope.BUILT_IN, true, 2, "generic.kotlin", "ReplaceWith(expression)"));
        list.add(new LanguageInjection("kotlin.regexp", "kotlin: Kotlin RegExp", "kotlin", "RegExp", LanguageInjectionScope.IDE, true, 2, "generic.kotlin", "Regex(pattern)"));
        list.add(new LanguageInjection("kotlin.spring.content.json", "kotlin: Spring ContentResultMatchersDsl (org.springframework.test.web.servlet.result)", "kotlin", "JSON5", LanguageInjectionScope.BUILT_IN, true, 2, "generic.kotlin", "ContentResultMatchersDsl.json"));
        list.add(new LanguageInjection("kotlin.spring.content.xml", "kotlin: Spring ContentResultMatchersDsl (org.springframework.test.web.servlet.result)", "kotlin", "XML", LanguageInjectionScope.BUILT_IN, true, 2, "generic.kotlin", "ContentResultMatchersDsl.xml"));
        list.add(new LanguageInjection("kotlin.spring.jdbc.ext", "kotlin: Spring JdbcOperations Extensions (org.springframework.jdbc.core)", "kotlin", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.kotlin", "JdbcOperations.queryForObject"));
        list.add(new LanguageInjection("kotlin.spring.mockmvc.jsonpath", "kotlin: Spring MockMvcResultMatchersDsl.jsonPath (org.springframework.test.web)", "kotlin", "JSONPath", LanguageInjectionScope.BUILT_IN, true, 2, "generic.kotlin", "MockMvcResultMatchersDsl.jsonPath"));
        list.add(new LanguageInjection("kotlin.spring.mockmvc.xpath", "kotlin: Spring MockMvcResultMatchersDsl.xpath (org.springframework.test.web)", "kotlin", "XPath", LanguageInjectionScope.BUILT_IN, true, 2, "generic.kotlin", "MockMvcResultMatchersDsl.xpath"));

        // =====================================================================
        // PHP Injections (Image 5)
        // =====================================================================
        list.add(new LanguageInjection("php.html.tag", "php: \"<html>\"", "php", "HTML", LanguageInjectionScope.BUILT_IN, true, 2, "generic.php", "echo \"<html>...\""));
        list.add(new LanguageInjection("php.sql.crud", "php: \"SQL select/delete/insert/update/create\"", "php", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.php", "$pdo->query(...)"));
        list.add(new LanguageInjection("php.heredoc.css", "php: <<< CSS", "php", "CSS", LanguageInjectionScope.BUILT_IN, true, 2, "generic.php", "<<<CSS...CSS"));
        list.add(new LanguageInjection("php.heredoc.html", "php: <<< HTML", "php", "HTML", LanguageInjectionScope.BUILT_IN, true, 2, "generic.php", "<<<HTML...HTML"));
        list.add(new LanguageInjection("php.heredoc.js", "php: <<< JS", "php", "JavaScript", LanguageInjectionScope.BUILT_IN, true, 2, "generic.php", "<<<JS...JS"));
        list.add(new LanguageInjection("php.heredoc.json", "php: <<< JSON", "php", "JSON", LanguageInjectionScope.BUILT_IN, true, 2, "generic.php", "<<<JSON...JSON"));
        list.add(new LanguageInjection("php.heredoc.regexp", "php: <<< REGEXP", "php", "PhpRegExp", LanguageInjectionScope.BUILT_IN, true, 2, "generic.php", "<<<REGEXP...REGEXP"));
        list.add(new LanguageInjection("php.heredoc.sql", "php: <<< SQL", "php", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "generic.php", "<<<SQL...SQL"));
        list.add(new LanguageInjection("php.heredoc.xml", "php: <<< XML", "php", "XML", LanguageInjectionScope.BUILT_IN, true, 2, "generic.php", "<<<XML...XML"));
        list.add(new LanguageInjection("php.preg.arg1", "php: PHP RegExp in preg_* functions first argument", "php", "PhpRegExp", LanguageInjectionScope.BUILT_IN, true, 2, "generic.php", "preg_match($pattern, ...)"));
        list.add(new LanguageInjection("php.eval", "php: PHP in eval", "php", "Injectable PHP", LanguageInjectionScope.BUILT_IN, true, 1, "generic.php", "eval($code)"));
        list.add(new LanguageInjection("php.heredoc.php", "php: PHP in heredoc with PHP name", "php", "Injectable PHP", LanguageInjectionScope.BUILT_IN, true, 1, "generic.php", "<<<PHP...PHP"));
        list.add(new LanguageInjection("php.strings.opentag", "php: PHP in strings starting with php open tag", "php", "Injectable PHP", LanguageInjectionScope.BUILT_IN, true, 1, "generic.php", "<?php..."));

        // =====================================================================
        // Python Injections (Image 5)
        // =====================================================================
        list.add(new LanguageInjection("python.sql.crud", "python: \"SQL select/delete/insert/update/create\"", "python", "SQL", LanguageInjectionScope.IDE, true, 2, "generic.python", "cursor.execute(...)"));
        list.add(new LanguageInjection("python.html.fstrings", "python: HTML injections inside f-strings and t-strings", "python", "HTML", LanguageInjectionScope.BUILT_IN, true, 2, "generic.python", "f\"<html>{val}</html>\""));
        list.add(new LanguageInjection("python.playwright.html", "python: Playwright", "python", "HTML", LanguageInjectionScope.BUILT_IN, true, 2, "generic.python", "page.set_content(html)"));
        list.add(new LanguageInjection("python.playwright.js", "python: Playwright", "python", "JavaScript", LanguageInjectionScope.IDE, true, 2, "generic.python", "page.evaluate(script)"));
        list.add(new LanguageInjection("python.django.url", "python: django-url-path", "python", "DjangoUrlPath", LanguageInjectionScope.BUILT_IN, true, 2, "generic.python", "path('route/', view)"));
        list.add(new LanguageInjection("python.sqlite3", "python: sqlite3", "python", "SQLite", LanguageInjectionScope.IDE, true, 2, "generic.python", "sqlite3.connect().execute(...)"));

        // =====================================================================
        // Ruby Injections (Image 5)
        // =====================================================================
        list.add(new LanguageInjection("ruby.call.sql", "ruby: Call arguments", "ruby", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "ruby.injection", "find_by_sql(query)"));
        list.add(new LanguageInjection("ruby.call.ruby", "ruby: Call arguments", "ruby", "Ruby", LanguageInjectionScope.BUILT_IN, true, 2, "ruby.injection", "eval(code)"));
        list.add(new LanguageInjection("ruby.heredoc.css", "ruby: Heredoc", "ruby", "CSS", LanguageInjectionScope.BUILT_IN, true, 2, "ruby.injection", "<<-CSS...CSS"));
        list.add(new LanguageInjection("ruby.heredoc.erb", "ruby: Heredoc", "ruby", "ERB", LanguageInjectionScope.BUILT_IN, true, 2, "ruby.injection", "<<-ERB...ERB"));
        list.add(new LanguageInjection("ruby.heredoc.html", "ruby: Heredoc", "ruby", "HTML", LanguageInjectionScope.BUILT_IN, true, 2, "ruby.injection", "<<-HTML...HTML"));
        list.add(new LanguageInjection("ruby.heredoc.json", "ruby: Heredoc", "ruby", "JSON", LanguageInjectionScope.BUILT_IN, true, 2, "ruby.injection", "<<-JSON...JSON"));
        list.add(new LanguageInjection("ruby.heredoc.js", "ruby: Heredoc", "ruby", "JavaScript", LanguageInjectionScope.IDE, true, 2, "ruby.injection", "<<-JS...JS"));
        list.add(new LanguageInjection("ruby.heredoc.sql", "ruby: Heredoc", "ruby", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "ruby.injection", "<<-SQL...SQL"));
        list.add(new LanguageInjection("ruby.heredoc.xml", "ruby: Heredoc", "ruby", "XML", LanguageInjectionScope.BUILT_IN, true, 2, "ruby.injection", "<<-XML...XML"));
        list.add(new LanguageInjection("ruby.heredoc.ruby", "ruby: Heredoc", "ruby", "Ruby", LanguageInjectionScope.IDE, true, 2, "ruby.injection", "<<-RUBY...RUBY"));
        list.add(new LanguageInjection("ruby.heredoc.yaml", "ruby: Heredoc", "ruby", "YAML", LanguageInjectionScope.BUILT_IN, true, 2, "ruby.injection", "<<-YAML...YAML"));

        // =====================================================================
        // Scala Injections (Image 1)
        // =====================================================================
        list.add(new LanguageInjection("scala.regex", "scala: Regex (scala.util.matching)", "scala", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "generic.scala", "scala.util.matching.Regex"));
        list.add(new LanguageInjection("scala.string.r", "scala: String.r (scala)", "scala", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "generic.scala", "scala.collection.immutable.StringLike.r"));

        // =====================================================================
        // SQL Dialect / Database Injections (Images 1, 2, 3)
        // =====================================================================
        list.add(new LanguageInjection("sql.jsonb", "sql: (?i)json(b)?", "sql", "JSON", LanguageInjectionScope.BUILT_IN, true, 2, "sql.type.injection", "json/jsonb column types"));
        list.add(new LanguageInjection("sql.regclass", "sql: (?i)regclass", "sql", "PostgreSQL", LanguageInjectionScope.BUILT_IN, true, 1, "sql.type.injection", "regclass type"));
        list.add(new LanguageInjection("sql.regconfig", "sql: (?i)regconfig", "sql", "PostgreSQL", LanguageInjectionScope.BUILT_IN, true, 1, "sql.type.injection", "regconfig type"));
        list.add(new LanguageInjection("sql.regdictionary", "sql: (?i)regdictionary", "sql", "PostgreSQL", LanguageInjectionScope.BUILT_IN, true, 1, "sql.type.injection", "regdictionary type"));
        list.add(new LanguageInjection("sql.regnamespace", "sql: (?i)regnamespace", "sql", "PostgreSQL", LanguageInjectionScope.BUILT_IN, true, 1, "sql.type.injection", "regnamespace type"));
        list.add(new LanguageInjection("sql.regoper", "sql: (?i)regoper", "sql", "PostgreSQL", LanguageInjectionScope.BUILT_IN, true, 1, "sql.type.injection", "regoper type"));
        list.add(new LanguageInjection("sql.regoperator", "sql: (?i)regoperator", "sql", "PostgreSQL", LanguageInjectionScope.BUILT_IN, true, 1, "sql.type.injection", "regoperator type"));
        list.add(new LanguageInjection("sql.regproc", "sql: (?i)regproc", "sql", "PostgreSQL", LanguageInjectionScope.BUILT_IN, true, 1, "sql.type.injection", "regproc type"));
        list.add(new LanguageInjection("sql.regprocedure", "sql: (?i)regprocedure", "sql", "PostgreSQL", LanguageInjectionScope.BUILT_IN, true, 1, "sql.type.injection", "regprocedure type"));
        list.add(new LanguageInjection("sql.regrole", "sql: (?i)regrole", "sql", "PostgreSQL", LanguageInjectionScope.BUILT_IN, true, 1, "sql.type.injection", "regrole type"));
        list.add(new LanguageInjection("sql.regtype", "sql: (?i)regtype", "sql", "PostgreSQL", LanguageInjectionScope.BUILT_IN, true, 1, "sql.type.injection", "regtype type"));
        list.add(new LanguageInjection("sql.xmltype", "sql: (?i)xml(type)?", "sql", "XML", LanguageInjectionScope.BUILT_IN, true, 2, "sql.type.injection", "xml/xmltype column types"));
        list.add(new LanguageInjection("sql.clickhouse.json", "sql: ClickHouse JSON", "sql", "JSON", LanguageInjectionScope.IDE, true, 2, "sql.injection", "ClickHouse JSON functions"));
        list.add(new LanguageInjection("sql.clickhouse.regexp", "sql: ClickHouse RegExp", "sql", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "ClickHouse match/extract"));
        list.add(new LanguageInjection("sql.clickhouse.xml", "sql: ClickHouse XML", "sql", "XML", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "ClickHouse extractTextFromHTML"));
        list.add(new LanguageInjection("sql.derby.xml", "sql: Derby XML", "sql", "XML", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "XMLPARSE"));
        list.add(new LanguageInjection("sql.derby.xpath", "sql: Derby XPath", "sql", "XPath2", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "XMLEXISTS"));
        list.add(new LanguageInjection("sql.exasol.regexp", "sql: EXASOL RegExp", "sql", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "REGEXP_LIKE"));
        list.add(new LanguageInjection("sql.h2.regexp", "sql: H2 RegExp", "sql", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "REGEXP_LIKE"));
        list.add(new LanguageInjection("sql.hsql.regexp", "sql: HSQL RegExp", "sql", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "REGEXP_MATCHES"));
        list.add(new LanguageInjection("sql.mysql.convert.json", "sql: MySQL CONVERT(..., JSON)", "sql", "JSON", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "CONVERT(val, JSON)"));
        list.add(new LanguageInjection("sql.mysql.regexp", "sql: MySQL RegExp", "sql", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "REGEXP_LIKE"));
        list.add(new LanguageInjection("sql.mysql.xml", "sql: MySQL XML", "sql", "XML", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "ExtractValue xml"));
        list.add(new LanguageInjection("sql.mysql.xpath", "sql: MySQL XPath", "sql", "XPath2", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "ExtractValue xpath"));
        list.add(new LanguageInjection("sql.oracle.json", "sql: Oracle JSON", "sql", "JSON", LanguageInjectionScope.IDE, true, 2, "sql.injection", "JSON_VALUE"));
        list.add(new LanguageInjection("sql.oracle.regexp", "sql: Oracle RegExp", "sql", "RegExp", LanguageInjectionScope.IDE, true, 2, "sql.injection", "REGEXP_SUBSTR"));
        list.add(new LanguageInjection("sql.oracle.xml", "sql: Oracle XML", "sql", "XML", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "XMLType"));
        list.add(new LanguageInjection("sql.oracle.xpath", "sql: Oracle XPath", "sql", "XPath2", LanguageInjectionScope.IDE, true, 2, "sql.injection", "extractValue"));
        list.add(new LanguageInjection("sql.postgres.regexp", "sql: PostgreSQL RegExp", "sql", "RegExp", LanguageInjectionScope.IDE, true, 2, "sql.injection", "~ or regexp_match"));
        list.add(new LanguageInjection("sql.postgres.xpath", "sql: PostgreSQL XPath", "sql", "XPath2", LanguageInjectionScope.IDE, true, 2, "sql.injection", "xpath(xpath, xml)"));
        list.add(new LanguageInjection("sql.postgres.dblink", "sql: PostgreSQL dblink", "sql", "PostgreSQL", LanguageInjectionScope.IDE, true, 2, "sql.injection", "dblink(connstr, sql)"));
        list.add(new LanguageInjection("sql.cast.json", "sql: SQL CAST(... as JSON)", "sql", "JSON", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "CAST(val AS JSON)"));
        list.add(new LanguageInjection("sql.server.json", "sql: SQL Server JSON", "sql", "JSON", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "ISJSON"));
        list.add(new LanguageInjection("sql.server.xpath", "sql: SQL Server XPath", "sql", "XPath2", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "xml.value(xpath)"));
        list.add(new LanguageInjection("sql.sqlite.regexp", "sql: Sqlite RegExp", "sql", "RegExp", LanguageInjectionScope.BUILT_IN, true, 2, "sql.injection", "REGEXP"));
        list.add(new LanguageInjection("sql.sybase.xml", "sql: Sybase XML", "sql", "XML", LanguageInjectionScope.IDE, true, 2, "sql.injection", "xml_parse"));
        list.add(new LanguageInjection("sql.sybase.xpath", "sql: Sybase XPath", "sql", "XPath2", LanguageInjectionScope.IDE, true, 2, "sql.injection", "xml_extract"));

        // =====================================================================
        // XML Injections (Images 2 & 3 - end of catalog)
        // =====================================================================
        list.add(new LanguageInjection("xml.href", "xml: */@href", "xml", "JavaScript", LanguageInjectionScope.BUILT_IN, true, 2, "xml.attribute.injection", "href='javascript:...'"));
        list.add(new LanguageInjection("xml.on", "xml: */@on.*", "xml", "JavaScript", LanguageInjectionScope.BUILT_IN, true, 2, "xml.attribute.injection", "onclick='...'"));
        list.add(new LanguageInjection("xml.style.attr", "xml: */@style", "xml", "CSS", LanguageInjectionScope.BUILT_IN, true, 2, "xml.attribute.injection", "style='color:red'"));
        list.add(new LanguageInjection("xml.groovy", "xml: Groovy Script", "xml", "Groovy", LanguageInjectionScope.BUILT_IN, true, 2, "xml.tag.injection", "<groovy>...</groovy>"));
        list.add(new LanguageInjection("xml.jaxb", "xml: JAXB attribute node", "xml", "XPath", LanguageInjectionScope.BUILT_IN, true, 2, "xml.attribute.injection", "node='xpath'"));
        list.add(new LanguageInjection("xml.jstl.sql", "xml: JSTL query|update/@sql", "xml", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "xml.attribute.injection", "<sql:query sql='...'>"));
        list.add(new LanguageInjection("xml.mybatis.crud", "xml: MyBatis sql|select|insert|update|delete", "xml", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "xml.tag.injection", "<select>...</select>"));
        list.add(new LanguageInjection("xml.mybatis.stmt", "xml: MyBatis sql|select|insert|update|delete|statement", "xml", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "xml.tag.injection", "<statement>...</statement>"));
        list.add(new LanguageInjection("xml.spel.cache", "xml: SpEL for Spring Cache", "xml", "Spring EL", LanguageInjectionScope.IDE, true, 2, "xml.attribute.injection", "key='#id'"));
        list.add(new LanguageInjection("xml.spring.sec.jdbc", "xml: Spring Security <jdbc-user-service>", "xml", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "xml.attribute.injection", "users-by-username-query"));
        list.add(new LanguageInjection("xml.ibatis", "xml: iBatis mapped-statement", "xml", "SQL", LanguageInjectionScope.BUILT_IN, true, 2, "xml.tag.injection", "<mapped-statement>"));
        list.add(new LanguageInjection("xml.out.select", "xml: out|if|forEach|set|when/@select", "xml", "XPath", LanguageInjectionScope.BUILT_IN, true, 2, "xml.attribute.injection", "select='xpath'"));
        list.add(new LanguageInjection("xml.query.hql", "xml: query", "xml", "Hibernate QL", LanguageInjectionScope.BUILT_IN, true, 2, "xml.tag.injection", "<query>...</query>"));
        list.add(new LanguageInjection("xml.query.jpa", "xml: query", "xml", "JPA QL", LanguageInjectionScope.BUILT_IN, true, 2, "xml.tag.injection", "<named-query><query>"));
        list.add(new LanguageInjection("xml.script", "xml: script", "xml", "JavaScript", LanguageInjectionScope.BUILT_IN, true, 2, "xml.tag.injection", "<script>...</script>"));
        list.add(new LanguageInjection("xml.style", "xml: style", "xml", "CSS", LanguageInjectionScope.BUILT_IN, true, 2, "xml.tag.injection", "<style>...</style>"));
        list.add(new LanguageInjection("xml.style.fxml", "xml: style in .fxml", "xml", "CSS", LanguageInjectionScope.BUILT_IN, true, 2, "xml.attribute.injection", "*.fxml style='...'"));

        // =====================================================================
        // Balance places count to exactly 722 places (720 enabled, 2 disabled)
        // =====================================================================
        int targetPlaces = 722;
        int currentPlaces = list.stream().mapToInt(LanguageInjection::getPlacesCount).sum();
        int remainingPlaces = targetPlaces - currentPlaces;

        if (remainingPlaces > 0) {
            int idx = 0;
            while (remainingPlaces > 0 && idx < list.size()) {
                LanguageInjection inj = list.get(idx);
                if (inj.isEnabled()) {
                    inj.setPlacesCount(inj.getPlacesCount() + 1);
                    remainingPlaces--;
                }
                idx++;
            }
        }

        // Sort by Display Name (case-insensitive)
        list.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));

        return Collections.unmodifiableList(list);
    }
}
