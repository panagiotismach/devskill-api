package com.devskill.devskill_api.models;

import jakarta.persistence.*;


@Entity
@Table(name = "libraries_changed")
public class LibraryChanged {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "library_name", nullable = false)
    private String libraryName;

    @Column(name = "version")
    private String version;

    @ManyToOne
    @JoinColumn(name = "commit_id", nullable = false)
    private Commit commit;

    // Constructors, Getters, Setters, Equals, and Hashcode

    public LibraryChanged() {
    }

    public LibraryChanged(String libraryName, String version, Commit commit) {
        this.libraryName = libraryName;
        this.version = version;
        this.commit = commit;
    }
}


