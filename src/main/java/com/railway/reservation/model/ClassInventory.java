package com.railway.reservation.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Base64;
import java.util.BitSet;

/**
 * Seats sold through channels outside this system for one class, quota and
 * departure (simulated demand), so availability reflects a busy network
 * rather than an empty train. Occupied berths are held for the whole run.
 */
@Entity
@Table(name = "class_inventory", uniqueConstraints =
        @UniqueConstraint(columnNames = {"train_class_id", "train_start_date", "quota"}))
public class ClassInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_class_id")
    private TrainClass trainClass;

    @Column(name = "train_start_date", nullable = false)
    private LocalDate trainStartDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Quota quota;

    @Lob
    @Column(name = "external_berths", nullable = false)
    private String externalBerths = "";

    @Column(name = "external_rac", nullable = false)
    private int externalRac;

    @Column(name = "external_wl", nullable = false)
    private int externalWl;

    protected ClassInventory() {}

    public ClassInventory(TrainClass trainClass, LocalDate trainStartDate, Quota quota) {
        this.trainClass = trainClass;
        this.trainStartDate = trainStartDate;
        this.quota = quota;
    }

    public BitSet externalSet() {
        return externalBerths.isEmpty() ? new BitSet() : BitSet.valueOf(Base64.getDecoder().decode(externalBerths));
    }

    public void setExternalSet(BitSet set) {
        this.externalBerths = Base64.getEncoder().encodeToString(set.toByteArray());
    }

    public Long getId() { return id; }
    public TrainClass getTrainClass() { return trainClass; }
    public LocalDate getTrainStartDate() { return trainStartDate; }
    public Quota getQuota() { return quota; }
    public int getExternalRac() { return externalRac; }
    public void setExternalRac(int externalRac) { this.externalRac = externalRac; }
    public int getExternalWl() { return externalWl; }
    public void setExternalWl(int externalWl) { this.externalWl = externalWl; }
}
