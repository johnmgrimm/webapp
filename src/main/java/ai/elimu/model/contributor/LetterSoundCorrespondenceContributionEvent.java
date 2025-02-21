package ai.elimu.model.contributor;

import ai.elimu.model.BaseEntity;
import ai.elimu.model.content.LetterSoundCorrespondence;
import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

@Entity
public class LetterSoundCorrespondenceContributionEvent extends BaseEntity {

    @NotNull
    @ManyToOne
    private Contributor contributor;

    @NotNull
    @ManyToOne
    private LetterSoundCorrespondence letterSoundCorrespondence;

    @NotNull
    private Integer revisionNumber;

    @Column(length = 1000)
    private String comment;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    private Calendar time;

    /**
     * The time passed during the creation/editing of the {@link #letterSoundCorrespondence}.
     */
    @NotNull
    private Long timeSpentMs;

    public String getComment() {
        return comment;
    }

    public Contributor getContributor() {
        return contributor;
    }

    public Calendar getTime() {
        return time;
    }

    public LetterSoundCorrespondence getLetterSoundCorrespondence() {
        return letterSoundCorrespondence;
    }

    public Integer getRevisionNumber() {
        return revisionNumber;
    }

    public void setRevisionNumber(Integer revisionNumber) {
        this.revisionNumber = revisionNumber;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setContributor(Contributor contributor) {
        this.contributor = contributor;
    }

    public void setTime(Calendar time) {
        this.time = time;
    }

    public void setLetterSoundCorrespondence(LetterSoundCorrespondence letterSoundCorrespondence) {
        this.letterSoundCorrespondence = letterSoundCorrespondence;
    }

    public Long getTimeSpentMs() {
        return timeSpentMs;
    }

    public void setTimeSpentMs(Long timeSpentMs) {
        this.timeSpentMs = timeSpentMs;
    }
}
