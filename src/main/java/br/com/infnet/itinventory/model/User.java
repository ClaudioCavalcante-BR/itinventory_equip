package br.com.infnet.itinventory.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(unique = true, nullable = false, name = "email")
    private String email;

    @Column(name = "dominio")
    private String dominio;

    @Column(nullable = false, name = "password")
    private String password;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_profile", nullable = false) // FK em users
    private Profile profile; // ADMIN, GESTOR_TI, USUARIO
}
