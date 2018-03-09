package ru.hh.school.adaptation;

import mocks.HardWorkerMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.core.CoreCommonConfig;
import ru.hh.nab.core.util.FileSettings;
import ru.hh.nab.hibernate.DataSourceFactory;
import ru.hh.nab.hibernate.HibernateCommonConfig;
import ru.hh.nab.hibernate.datasource.DataSourceType;
import ru.hh.school.adaptation.hard.work.HardWorker;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Configuration
@Import({
        CoreCommonConfig.class,
        HibernateCommonConfig.class,
        HardWorkerMock.class,
        DistributorDao.class,
        ExampleResource.class})
public class TestConfig {

    @Bean
    FileSettings fileSettings() throws IOException {
        Properties properties = new Properties();
        try (InputStream resource = getClass().getResourceAsStream("/service.properties")) {
            properties.load(resource);
        }

        return new FileSettings(properties);
    }

    @Bean
    DataSource dataSource(DataSourceFactory dataSourceFactory) {
        return dataSourceFactory.create(DataSourceType.DEFAULT);
    }

    @Bean
    DataSourceFactory dataSourceFactory(FileSettings fileSettings, String serviceName, StatsDSender statsDSender) {
        return new DataSourceFactory(fileSettings, serviceName, statsDSender);
    }

    @Bean
    Properties hibernateProperties(FileSettings fileSettings) {
        return fileSettings.getSubProperties("hibernate");
    }
}
