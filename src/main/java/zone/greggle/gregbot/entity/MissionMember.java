package zone.greggle.gregbot.entity;

public class MissionMember {

    public Long discordID;
    public String missionRole;
    public boolean selectingRole;

    public MissionMember(Long discordID) {
        this.discordID = discordID;
        this.missionRole = "Not Selected";
        this.selectingRole = false;
    }

    public Long getDiscordID() {
        return discordID;
    }
    public void setDiscordID(Long discordID) {
        this.discordID = discordID;
    }

    public String getMissionRole() {
        return missionRole;
    }
    public void setMissionRole(String missionRole) {
        this.missionRole = missionRole;
    }

    public boolean isSelectingRole() {
        return selectingRole;
    }
    public void setSelectingRole(boolean selectingRole) {
        this.selectingRole = selectingRole;
    }
}
