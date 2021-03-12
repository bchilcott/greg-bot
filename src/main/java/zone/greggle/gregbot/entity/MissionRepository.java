package zone.greggle.gregbot.entity;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import zone.greggle.gregbot.data.EditMode;

import java.util.List;

@Repository
public interface MissionRepository extends MongoRepository<Mission, String> {

    Mission findMissionById(String id);

    Mission findByMissionChannelID(long missionChannelID);

    void deleteById(@NotNull String id);

    List<Mission> findByPublishedIsFalseAndEditModeIsNot(EditMode editMode);

    List<Mission> findByPublishedIs(boolean isPublished);

    @Query(value = "{ 'members.discordID': ?0 }")
    List<Mission> findMissionsByMemberDiscordID(Long discordID);
}
