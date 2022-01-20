package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

import static javax.persistence.FetchType.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age"}) // 연관관계 필드는 손을 안대는게 좋다.
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String username;
    private int age;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "team_id")
    private Team team;


    public Member(String username){
        this.username = username;
    }

    public Member(String username, int age) {
        this.username = username;
        this.age = age;
    }



    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }

    }

    private void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }


}


