package zone.greggle.gregbot.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import zone.greggle.gregbot.entity.Mission;
import zone.greggle.gregbot.entity.MissionRepository;

//@RestController
public class MissionController {
    private static final Logger logger = LoggerFactory.getLogger(MissionController.class);

    @Autowired
    MissionRepository missionRepository;

    @GetMapping(value = "/mission")
    public Mission getMissionByShortID(String shortID) {
        logger.info(shortID);
        return missionRepository.findByShortID(shortID);
    }

}
