package com.example.aggridapi;

import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@SpringBootApplication
public class AgGridApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgGridApiApplication.class, args);
    }

}

@Configuration
@EnableWebFlux
class CorsGlobalConfiguration implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {
        corsRegistry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods(HttpMethod.GET.name())
                .maxAge(3600);
    }

}

@RequiredArgsConstructor
@RestController
class CarController {

    private final CarService carService;
    private final AthleteService athleteService;

    @GetMapping("/api/cars")
    public Flux<CarDto> getCars() {
        return Flux.fromStream(carService.getCars().stream());
    }

    @GetMapping("/api/athletes")
    public Flux<AthleteDto> getAthletes(@RequestParam(required = false) String medal) {
        return athleteService.getAthletes(medal);
    }

}

@Service
class CarService {

    private List<String> makes = List.of("BMW", "Audi", "Toyota", "Chevy", "Ford", "Dodge", "Lincoln", "Buick", "Honda", "Nissan");

    private Map<String, List<String>> modelsByMake = new HashMap<>();

    List<String> bmwModels = List.of("328i", "M3", "M5", "X1", "X3", "X5");
    List<String> audiModels = List.of("A4", "A5", "S5", "A7", "A8");
    List<String> toyotaModels = List.of("Prius", "Camry", "Corolla");
    List<String> chevyModels = List.of("Camero", "Silverado", "Malibu");
    List<String> fordModels = List.of("Mustang", "F150", "Focus", "Fiesta");
    List<String> dodgeModels = List.of("Ram", "Challenger", "Charger", "Durango");
    List<String> lincolnModels = List.of("Navigator", "MKZ", "MKX", "MKS");
    List<String> buickModels = List.of("Enclave", "Regal", "LaCrosse", "Verano", "Encore", "Riveria");
    List<String> hondaModels = List.of("Accord", "Civic", "CR-V", "Odyssey");
    List<String> nissanModels = List.of("Rogue", "Juke", "Cube", "Pathfinder", "Versa", "Altima");
    
    private Faker faker = new Faker(Locale.ENGLISH);
    private List<CarDto> cars;

    @PostConstruct
    public void init() {
        modelsByMake.put("BMW", bmwModels);
        modelsByMake.put("Audi", audiModels);
        modelsByMake.put("Toyota", toyotaModels);
        modelsByMake.put("Chevy", chevyModels);
        modelsByMake.put("Ford", fordModels);
        modelsByMake.put("Dodge", dodgeModels);
        modelsByMake.put("Lincoln", lincolnModels);
        modelsByMake.put("Buick", buickModels);
        modelsByMake.put("Honda", hondaModels);
        modelsByMake.put("Nissan", nissanModels);

        cars = new ArrayList<>();

        for (String make: makes) {

            for (String model: modelsByMake.get(make)) {
                int price = faker.number().numberBetween(30,81)*1000;

                cars.add(new CarDto(make, model, price));
            }

        }
    }

    public List<CarDto> getCars() {
        return this.cars;
    }

}

@Slf4j
@Service
class AthleteService {

    private static final String COMMA_DELIMITER = ",";
    @Value("classpath:/athlete_events.csv")
    private Resource athletesCsv;

    private Flux<AthleteDto> athletes = Flux.empty();

    @PostConstruct
    public void init() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(athletesCsv.getFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(COMMA_DELIMITER);
                if (values.length==15) {
                    String id = values[0].replaceAll("\"", "");
                    String name = values[1].replaceAll("\"", "");
                    String gender = values[2].replaceAll("\"", "");
                    Number age = getInt(values[3].replaceAll("\"", ""));
                    Number height = getInt(values[4].replaceAll("\"", ""));
                    Number weight = getInt(values[5].replaceAll("\"", ""));
                    String team = values[6].replaceAll("\"", "");
                    String noc = values[7].replaceAll("\"", "");
                    String games = values[8].replaceAll("\"", "");
                    String year = values[9].replaceAll("\"", "");
                    String season = values[10].replaceAll("\"", "");
                    String city = values[11].replaceAll("\"", "");
                    String sport = values[12].replaceAll("\"", "");
                    String event = values[13].replaceAll("\"", "");
                    String medal = values[14].replaceAll("\"", "");

                    AthleteDto dto = new AthleteDto(id, name, gender, age, height, weight, team, noc, games, year, season, city, sport, event, medal);

                    athletes = athletes.concatWithValues(dto);
                }
            }

        }
    }

    private Number getInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    public Flux<AthleteDto> getAthletes(String medal) {
        if (medal == null) {
            return athletes.take(50000);
        }
        else return athletes.filter(athleteDto -> athleteDto.medal().equalsIgnoreCase(medal));
    }
}

record CarDto(String make, String model, int price) {}

record AthleteDto(String id, String name, String gender, Number age, Number height, Number weight, String team, String noc, String games, String year, String season, String city, String sport, String event, String medal) {}

