package io.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Slf4j
@Configuration
@EnableTransactionManagement
@PropertySource("classpath:application.properties")
public class DataBaseConfig {

    private HikariDataSource hikariDataSource;

    @Value("${DB_URL}")
    private String dbUrl;

    @Value("${DB_USER}")
    private String dbUser;

    @Value("${DB_PASSWORD}")
    private String dbPassword;

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Value("${DB_DRIVER:}")
    private String dbDriver;

    @Bean
    public DataSource dataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dbUrl);
        hikariConfig.setUsername(dbUser);
        hikariConfig.setPassword(dbPassword);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setDriverClassName(dbDriver);

        this.hikariDataSource = new HikariDataSource(hikariConfig);
        log.info("✅ HikariCP pool initialized.");

        return this.hikariDataSource;
    }

    @PreDestroy
    public void destroy() {
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            log.info("Closing HikariCP DataSource...");
            hikariDataSource.close();
            log.info("✅ HikariCP DataSource closed.");
        }
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas("public")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        var current = flyway.info().current();
        log.info("Flyway migrations applied (schema version = {}).",
                current != null ? current.getVersion() : "not applied yet");

        return flyway;
    }

    @Bean
    public LocalSessionFactoryBean sessionFactoryBean(DataSource dataSource,
                                                      @Value("${hibernate.packagesToScan}") String packageToScan,
                                                      @Value("${hibernate.dialect}") String dialect,
                                                      @Value("${hibernate.hbm2ddl.auto:validate}") String hbm2ddlAuto) {
        log.info("Session factory and migration started");
        LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan(packageToScan);

        Properties properties = new Properties();
        properties.put("hibernate.dialect", dialect);
        properties.put("hibernate.show_sql", true);
        properties.put("hibernate.format_sql", true);
        properties.put("hibernate.hbm2ddl.auto", hbm2ddlAuto);
        factoryBean.setHibernateProperties(properties);

        return factoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(SessionFactory sessionFactory) {
        HibernateTransactionManager txManager = new HibernateTransactionManager();
        txManager.setSessionFactory(sessionFactory);
        return txManager;
    }

}
