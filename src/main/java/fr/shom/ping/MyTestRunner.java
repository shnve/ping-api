package fr.shom.ping;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;

@Component
public class MyTestRunner implements CommandLineRunner {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=========================================");
        System.out.println("DUMPING TRANSLATIONS:");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT enum_name, translation FROM warning_hazard_type_general WHERE enum_name = 'AIDS_TO_NAVIGATION'");
        for (Map<String, Object> row : rows) {
            System.out.println("ROW: " + row);
        }
        
        System.out.println("DUMPING LIQUIBASE:");
        List<Map<String, Object>> lrows = jdbcTemplate.queryForList("SELECT id, author, dateexecuted FROM databasechangelog WHERE id LIKE '%SHN%'");
        for (Map<String, Object> row : lrows) {
            System.out.println("LIQUIBASE: " + row);
        }
        System.out.println("=========================================");
    }
}
