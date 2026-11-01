package com.kms301.nkms.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "cap_the_doi_lap")
public class CapTheDoiLap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "the_a_id", nullable = false)
    private The theA;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "the_b_id", nullable = false)
    private The theB;

    @Transient
    private String moTa;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public The getTheA() { return theA; }
    public void setTheA(The theA) { this.theA = theA; }
    public The getTheB() { return theB; }
    public void setTheB(The theB) { this.theB = theB; }
    public String getMoTa() {
        if (moTa != null) return moTa;
        if (theA != null && theB != null) {
            return "Cặp thẻ đối lập giữa \"" + theA.getTen() + "\" và \"" + theB.getTen() + "\"";
        }
        return "Cặp thẻ đối lập";
    }
    public void setMoTa(String moTa) { this.moTa = moTa; }
}
