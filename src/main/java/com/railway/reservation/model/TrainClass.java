package com.railway.reservation.model;

import jakarta.persistence.*;

/** A class of accommodation on a train and how many coaches of it the rake carries. */
@Entity
@Table(name = "train_classes", uniqueConstraints = @UniqueConstraint(columnNames = {"train_id", "coach_class"}))
public class TrainClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_id")
    private Train train;

    @Enumerated(EnumType.STRING)
    @Column(name = "coach_class", nullable = false, length = 20)
    private CoachClass coachClass;

    @Column(nullable = false)
    private int coaches;

    protected TrainClass() {}

    public TrainClass(CoachClass coachClass, int coaches) {
        this.coachClass = coachClass;
        this.coaches = coaches;
    }

    public int capacity() { return coaches * coachClass.berthsPerCoach(); }

    /** Berths held back for the Tatkal quota: the highest-numbered berths of the class. */
    public int tatkalBerths() {
        if (!coachClass.supportsTatkal()) return 0;
        return (int) Math.floor(capacity() * (coachClass == CoachClass.SECOND_SITTING ? 0.10 : 0.20));
    }

    public int generalBerths() { return capacity() - tatkalBerths(); }

    public int racLimit(Quota quota) {
        if (!coachClass.supportsRac()) return 0;
        int all = coaches * coachClass.racPerCoach();
        return quota == Quota.TATKAL ? Math.max(1, all / 5) : all;
    }

    /** Waiting lists are capped at a quarter of the berths in the quota's pool. */
    public int waitlistLimit(Quota quota) {
        int pool = quota == Quota.TATKAL ? tatkalBerths() : generalBerths();
        return Math.max(5, pool / 4);
    }

    public Long getId() { return id; }
    public Train getTrain() { return train; }
    void setTrain(Train train) { this.train = train; }
    public CoachClass getCoachClass() { return coachClass; }
    public int getCoaches() { return coaches; }
    public void setCoaches(int coaches) { this.coaches = coaches; }
}
