package com.aurelia.grand.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "staff_profiles")
public class StaffProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String department; // 'Housekeeper','Room Service','Receptionist','Maintenance','Chef','Concierge'

    @Column(nullable = false, length = 60)
    private String zone = "All Floors";

    @Column(name = "shift_start", nullable = false)
    private LocalTime shiftStart;

    @Column(name = "shift_end", nullable = false)
    private LocalTime shiftEnd;

    @Column(name = "on_duty", nullable = false)
    private Integer onDuty = 1;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public LocalTime getShiftStart() { return shiftStart; }
    public void setShiftStart(LocalTime shiftStart) { this.shiftStart = shiftStart; }

    public LocalTime getShiftEnd() { return shiftEnd; }
    public void setShiftEnd(LocalTime shiftEnd) { this.shiftEnd = shiftEnd; }

    public Integer getOnDuty() { return onDuty; }
    public void setOnDuty(Integer onDuty) { this.onDuty = onDuty; }
}
