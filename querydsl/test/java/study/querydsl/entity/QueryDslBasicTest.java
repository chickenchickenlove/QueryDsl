package study.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory; // 필드로 빼서 쓰고, 주입해라.


    @BeforeEach
    public void beforeEach() {
        queryFactory = new JPAQueryFactory(em); // 스프링이 주입해주는 em은 멀티스레드에 아무 문제 없게 설계가 되어있어서 같이 써도 문제없음.

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);


        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void startJpql() throws Exception{
        // member1을 찾아라.
        Member findMember = em.createQuery("select m from Member m" +
                        " where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQueryDsl() throws Exception{




//        JPAQueryFactory queryFactory = new JPAQueryFactory(em); // 쿼리 팩토리 만들 때, 엔티티 매니저를 넘겨줘야한다. 그래야 데이터를 찾는다.
        // 위에 코드 빼고 필드로 가져간다.


        // 그냥 하면 Q값이 없다.
        // 이 때 그래들로 가서 CompilQueryDSl 해서 q 만들어준다.
        QMember m = new QMember("m"); // 엘리어스를 준다. 크게 중요하진 않다. 왜냐하면 이걸 안 쓸 것이기 때문임. QMember.member로 만들어져 있는 것임.
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1")) // 파라메터 바인딩을 자동으로 한다.
                .fetchOne();

        // 보면 where에 값이 잘 들어가는 걸 볼 수 있다. JDBC에 있는 Prepare statement롤 통해서 자동으로 파라메터 바인딩을 한다.
        // JPQL의 장점. QTYPE을 만들어주고, 그걸 바탕으로 자바를 바탕으로 코드를 짠다. 따라서 컴파일 시점에서 오류가 있으면 다 해결해준다.

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test // 스태틱 임포트로 하기 (미리 생성된 인스턴스를 쓰는 것임)
    void startQueryDsl_static() throws Exception{

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) // 파라메터 바인딩을 자동으로 한다.
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member) // select + from
                .where(member.username.eq("member1") // chain을 and나 or로 갈 수 있음.
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    public void searchAndParam(){
        Member findMember = queryFactory
                .selectFrom(member) // select + from
                .where(
                        member.username.eq("member1"), // and를 ','로 풀어서 할 수 있음.
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetchTest() {

        // getResultList()로 받는거.
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        // getResult() 단건 조회
//        Member member = queryFactory
//                .selectFrom(QMember.member)
//                .fetchOne();
//
//        // limit(1).getResultList()로 단건 조회
//        Member member1 = queryFactory
//                .selectFrom(QMember.member)
//                .fetchFirst();



        //쿼리가 2번 나간다.
        //먼저 페이징을 하기 위한 카운트를 세는 쿼리가 나간다.
        //그 다음에 컨텐츠를 가져오기 위한 쿼리가 나간다.
        QueryResults<Member> results = queryFactory
                .selectFrom(QMember.member)
                .fetchResults();

        results.getTotal(); // count 값이 나오는거.
        List<Member> content = results.getResults(); // 여기에 진짜 정보가 담겨있다.

        // JPQL에서 엔티티를 직접 지정하면, SQL에는 ID로 바뀐다.
        // 카운트 쿼리로 바뀐다.
        long total = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(DESC)
     * 2. 회원 이름 올림차순(ASC)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */


    @Test
    public void sort() {

        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast()
                )
                .fetch();


        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }


    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc()) // 페이징에서는 orderBy 반드시 넣어야함. 그래야 페이징이 잘 작동하는지 알 수 있음.
                .offset(0)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo((2));

    }
    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc()) // 페이징에서는 orderBy 반드시 넣어야함. 그래야 페이징이 잘 작동하는지 알 수 있음.
                .offset(0)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4); // 전체 사이즈 4개
        assertThat(queryResults.getLimit()).isEqualTo(2); // 리미트 2개 했으니 2개
        assertThat(queryResults.getOffset()).isEqualTo(1); // 리미트 1에 했음
        assertThat(queryResults.getResults().size()).isEqualTo(2); // 실제 만들어진 건 2개
    }

    @Test
    public void aggregation() {
        // member.count : 멤버가 몇 명이야.
        // select에서 내가 원하는 걸 하나씩 찍었다.
        // 이렇게 조회할 경우 Tuple로 조회하게 된다.
        // 데이터가 이렇게 여러개로 들어올 때는, tuple을 쓰게 된다.
        // 그런데 실무에서는 DTO로 대부분 쓰게 된다.
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        // 튜플은 위에 적은거 그대로 사용하면 된다.
        assertThat(tuple.get(member.count())).isEqualTo((4));
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }


    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void groupBy() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team) // 멤버의 team과 team 테이블을 조인한다.
                .groupBy(team.name) // 팀의 이름으로 그룹핑 한다.
//                .having() //having 조건도 가능함. groupBy한 결과 중에서 어떠어떠한 것만 뽑으라라고 한다.
                .fetch();


        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);


    }

    /**
     * 팀 A에 소속된 모든 회원
     */

    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
//                .leftJoin() // inner join, left join , outer join 등등이 가능하다.
                .join(member.team, team) // 엘리어스로 team을 쓴다는 소리
                .where(team.name.eq("teamA"))
                .fetch();

        //extracing 먼지 알아두기.
        assertThat(result).extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인 : 막~ 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * From 절에 여러 엔티티를 선택해서 세타 조인이 가능함.
     * 세타 조인에서는 outer 조인이 불가능하다.
     * ==> outer 조인을 하려면, 조인 on을 사용하면 외부 조인 가능함.
     */


    @Test // 연관관계가 없어도 Join이 된다.
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) // from 절을 2개를 나열한 거임.
                .where(member.username.eq(team.name))
                .fetch(); // 멤버 , 팀 테이블 두개를 가져온다. 그리고 멤버 이름 == 팀이름인 테이블을 가져온다.

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }


    /**
     * 조인 - on 절
     * on 절을 활용한 조인
     * 1. 조인 대상 필터링
     * 2. 연관관계 없는 엔티티 외부조인 --> 이거 쓸 때 많이 쓰임.
     */


    /**
     * 예 ) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL : select m,t from Member m left join m.team t on t.name = "teamA"
     * 쿼리 찍어보면, teamA만 있기 때문에 teamB에 대한 값들은 모두 null로 된다.
     * JPQL은 left join 이후에 'with'라는 문법이 들어간다.
     * 실제 SQL에서는 on 절이 들어간다.
     */

    @Test
    public void join_on_filtering() {

        // tuple로 나오는 이유는 select가 여러개가 나왔기 때문임.
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team) // 그냥 join하면, 내부조인이다. 따라서 null 값은 다 날아가버림.
                .on(team.name.eq("teamA"))
                .fetch();
        result.forEach(tuple -> System.out.println("tuple = " + tuple));
    }


    @Test
    public void join_on_filtering1() {
        // on 절 == where절인 경우.
        // 이 경우는 똑같은 결과가 나온다. inner join인 경우, on절이나 where이나 결과가 똑같이 나온다.
        /**
         * 내부 조인인 경우 무조건 where절로 해버린다.
         * 정말 외부 조인이 필요한 경우에만 on절을 사용하자.
         * on 절은 가져오는 데이터 자체가 줄어든다.
         *
         */

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team) // 그냥 join하면, 내부조인이다. 따라서 null 값은 다 날아가버림.
                .on(team.name.eq("teamA"))
                .fetch();

        List<Tuple> result1 = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
    }


    /**
     * on 절
     * 연관관계 없는 엔티티 외부 조인할 때 사용하기도 함.
     * 회원의 이름 = 팀 이름인 대상 외부 조인
     */

    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));


        // theta - join인 경우 Outer Join이 안되니, LeftJoin이 안된다.
        // on절에 맞는 오른쪽 테이블(Team)을 가져온 다음에 join을 시킨다.
        // 만족하지 않는 애들은 null값으로 나온다.
        List<Tuple> fetch = queryFactory
                .select(member, team)
                .from(member) // from 절을 2개를 나열한 거임.
                .leftJoin(team) // 막조인을 하니 team으로 간다. 연관관계가 있을 때는 member.team 이렇게 했다.
                // member.team = team으로 했으면 각 PK로 하는데.. 이렇게 할 경우, 그냥 TEAM끼리 조인한다.
                .on(member.username.eq(team.name)) // ON절은 JOIN 하는 대상을 줄여준다.
                .fetch();// 멤버 , 팀 테이블 두개를 가져온다. 그리고 멤버 이름 == 팀이름인 테이블을 가져온다.

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }


    /**
     * 페치 조인이 없을 때
     * 페인 조인 테스트 할 때는, 영속성 컨텍스트에 있는 걸 DB에 반영하고 날린다음에 시작하는게 낫다.
     */


//    @PersistenceContext
//    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        // Lazy Loading이니 미적용 되는 것이 맞음.
//        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
//        assertThat(loaded).as("페치 조인 미적용").isFalse();
        System.out.println("==================");
        findMember.getTeam().getName();
    }


    /**
     * fetch join : 왼쪽과 연관된 오른쪽을 전부 한방에 가져온다.
     */
    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() // 이렇게 fetchjoin을 넣어주면 된다.
                .where(member.username.eq("member1"))
                .fetchOne();

        System.out.println("==================");
        findMember.getTeam().getName();
    }


    /**
     * 서브 쿼리 : 쿼리 안에 쿼리를 사용한다.
     * JPAExpressions를 사용해주면 된다.
     */

    /**
     * 나이가 가장 많은 회원 조회
     *
     */

    @Test
    public void subQuery() {

        // JPAExpression은 서브쿼리다. 따라서 서브쿼리는 바깥의 member와 엘리어스가 겹치면 안된다.
        // 1. select from으로 member를 가져왔다.
        // 2. where 절에서 member의 나이가 서브쿼리와 같은거((member의 나이가 가장 큰 사람))
        // 1+2 를 통해서 값이 나온다.
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);

    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQueryGoe() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);

    }

    /**
     * 나이가 평균 이상인 회원
     */

    @Test
    public void subQueryIn() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20,30,40);

    }


    @Test
    public void selectSubQuery() {

        QMember memberSub = new QMember("memberSub");

        // user 이름 + 전체 user들의 평균 나이를 출력한다.

        List<Tuple> fetch = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }
    
    
    @Test
    public void basicCase() {

        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
        

    }


    /**
     * 복잡한 case문 : casBuilder 사용한다.
     */

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


    /**
     * 상수가 필요하면 Expressions를 사용한다.
     * JPQL에서는 상수 'A'가 나가지 않는다.(쿼리가 안 나감.
     */
    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 문자 더하기
     * member.age는 더하기가 안됨. 왜냐하면 타입이 다름.
     * stringValue로 문자열로 바꿔주라.
     * 쿼리 나가는 것도 한번 확인해보자. StringValue가 나가니 Casting 쿼리가 나가는 것도 볼 수 있음.
     */
    @Test
    public void concat() {

        //{userName}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


    /**
     * 프로젝션 : Select절에 올 값이 무엇인지 선택하는 것을 프로젝션이라고 함.
     * 프로젝션 : select 대상 지정
     * 프로젝션 대상이 하나면 타입을 명확하게 지정할 수 있음.
     * 프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회
     * 튜플은 여러 개의 타입이 들어올 때를 대비해서 막 만든 자료구조다.
     */

    @Test
    public void simpleProjection() {
        /**
         * 프로젝션 대상이 하나.
         * 하나의 값으로만 나옴.
         */

        List<String> fetch = queryFactory
                .select(member.username)
                .from(member)
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }


    @Test
    public void tupleProjection() {
        /**
         * 프로젝션 대상이 둘.
         * 튜플 값으로 나옴.
         */

        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            /**
             * tuple은 properties로 접근한다. 넣은 거 그대로
             */
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    /**
     * JPQL Query : new operation 문법으로 DTO 생성한다.
     * 순수 JPA에서 DTO를 조회할 때는 new 명령어를 사용해야함.
     * 패키지명 다 적어줘야해서 지저분함.
     * 생성자 방식만 지원함. (셋터나 이런거 안됨)
     */

    @Test
    public void findDtoByJpql() {
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age)" +
                " from Member m", MemberDto.class).getResultList();
        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * QueryDsl 빈 생성(Bean Population)
     * 결과를 DTO로 반환할 때 사용
     * 다음 3가지 방법 지원
     *
     * 1. 프로퍼티 접근 // 변수명 매칭이 매우 중요함. 변수명 다를 경우 .as()로 접근 + ExpressionUtils
     * 2. 필드 직접 접근 // 변수명 매칭이 매우 중요함. 변수명 다를 경우 .as()로 접근
     * 3. 생성자 사용 // 생성자는 순서를 보고 들어가기 때문에 이름이 달라도 됨.
     */


    /**
     * 프로퍼티 접근 방법(Setter를 이용함)
     * Projectdion.bean
     */
    @Test
    public void findDtoBySetter() {

        // bean이라고 하면 getter/setter의 bean을 말한다.
        // setter로 데이터를 인젝션 해준다.
        // 그리고 타입을 지정해줘야함.
        // 기본 생성자가 없으면 getConstructor0 같은 에러가 발생한다.

        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }



    }

    /**
     * Projections.fields
     * field에 값을 바로 꽂는다.
     * 따라서 @Getter, @Setter가 필요없다.
     */

    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 생성자 접근 방법
     * Projections.constructor
     *      * 문제점
     *      * 코드를 실행하는 순간이 되어서야 런타임 에러로 볼 수 있다.
     *      * QmemberDTo(member.username, member.age, member.id)
     *      * id라는게 생성자에 없는데 넣어버리면, 컴파일 단계에서는 알아 차릴 수 없고, 실행할 때 에러가 발생한다.
     */

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    /**
     * 필드는 username, userDto는 name으로 되어있음.
     * 이런 경우에는 에러가 나서 들어가지 않는다.
     * 매칭이 안되었기 때문에 값이 안들어가서 null로 된다.
     * 이걸 해결해주기 위핸 member.username.as('name')으로 맞춰준다.
     *
     */

      @Test
    public void findUserDtoByField() {
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class, member.username.as("name"), member.age))
                .from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }

    }


    /**
     * @QueryProjection
     * DTO에 생성자에 달아주면, 그걸 바탕으로 Q파일이 만들어짐.
     * 문제점
     * DTO가 QUERYDSL에 대한 의존성이 생긴다. 왜냐하면 DTO에 @QueryProjection이 들어갔다.
     * 즉, DTO가 QUERY DSL을 알고 있다.
     * 그런데 이 DTO는 보통 서비스, 어플리케이션 계층까지 올라가는데... 여기까지 이런 하부단 의존성을 가진 놈이 가는
     * 게 맞을까?
     * @QUERYProjection을 쓴다는 것은 이런 아키테쳐적인 문제점이 있다.
     *
     */


    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    /**
     * 동적 쿼리를 해결하는 두 가지 방식
     * 1. BooleanBuilder
     * 2. Where 다중 파라미터 사용
     */


    /**
     * Boolean Builder를 사용한 동적 쿼리 방식.
     */
    
    @Test
    public void dynamicQuery_BooleanBuilder() {
        // 검색조건으로 아래를 만족하는 사람을 찾고 싶음.
        String usernameParam = "member1";
        Integer ageParam = null; // null인 경우 usernameCond만 파라메터로 들어간다.

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    /**
     * where 다중 파라미터를 사용한 동적 쿼리
     * 메서드를 만들 때, 타입을 predicate가 아닌 BooleanExpression으로 만들면...
     * 이걸로 조립이 가능하다.
     * BooleanExpression으로 만들어도, 정상적으로 동작한다.
     */


    @Test
    public void dynamicQuery_whereParam() {
        // 검색조건으로 아래를 만족하는 사람을 찾고 싶음.
        String usernameParam = "member1";
        Integer ageParam = 10; // null인 경우 usernameCond만 파라메터로 들어간다.

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {

        /**
         * where 문 안에서 바로 해결한다.
         * where 문에 null이 들어오면, QueryDSL은 null을 무시한다.
         * 기본적으로 where절에 ','로 나열되게 되면 and로 들어가게 되는데, 이 때 null 조건은 무시한다.
         */

        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond)) // null을
//                .where(allEq(usernameCond, ageCond)) // 조립이 가능하다. 진짜 좋다.
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        // null 처리를 해준다.
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null; // 삼항 연산자로 반환하기
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }



    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        // 파라미터 값이 null이냐 아니냐에 따라 쿼리가 동적으로 바껴야 함.
        // null이면 where문에 조건문이 안 들어가야함.

        // BooleanBuilder()에는 초기값 넣을 수도 있다.
        // new BooleanBuilder(member.username.eq(usernameCond))


        BooleanBuilder builder = new BooleanBuilder();
        // 불린빌더는 and나 or로 조립을 할 수 있음.
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond)); // 불린빌더에 and 조건을 하나 넣어준다.
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder) // build 결과를 where절에 넣어준다. + builder도 and / or로 조립이 된다. 
                .fetch();
    }

    /**
     * 수정, 삭제 배치 쿼리
     *
     * 변경 감지 : 개별 엔티티 건건이 일어나는거다.
     * 따라서, 쿼리가 여러번 많이 나갈 것이다.
     *
     * 쿼리 한번으로 대량 데이터 수정하는 방법은? --> 수정, 삭제 배치쿼리!
     * ex) 모든 개발자의 연봉을 50% 인상해 같은 쿼리!
     */

    @Test
//    @Rollback(value = false)
    public void bulkUpdate() {

        // 멤버의 나이가 28살보다 낮으면, 모두 비회원으로 바꿀 것이다.
        // execute()로 실행하고, count는 영향을 받은 row가 나온다.

        /**
         * member1 = 10 -> 비회원
         * member2 = 20 -> 비회원
         * member3 = 30 -> 유지
         * member4 = 40 -> 유지
         * 쿼리가 2번 나간다.
         *
         * JPA는 영속성 컨텍스트에 엔티티가 다 올라와있다.
         */


        /**
         * 실행 전.
         * 1 member1 = 10 -> member
         * 2 member2 = 20 -> member
         * 3 member3 = 30 -> member
         * 4 member4 = 40 -> member
         */

        long count = queryFactory
                .update(member) // sql 업데이트와 같다.
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        // BULK 연산 후, DB와 영속성 컨텍스트의 괴리를 없애고 싶다면..
        // BULK 연산 후 항상 이 것을 해준다.
        // em.flush()
        // em.clear()


        /**
         * 실행 후.
         * 1 member1 = 10 -> 비회원
         * 2 member2 = 20 -> 비회원
         * 3 member3 = 30 -> member
         * 4 member4 = 40 -> member
         */

        List<Member> result = queryFactory
                .select(member)
                .fetch();


        // DB에서 가져온 값은 이미 영속성 컨텍스트에 있어서, 영속성 컨텍스틔 값이 우선이 된다.
        // 따라서 DB의 값이 아닌 영속성 컨텍스트의 값이 출력된다.
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }


    /**
     * JPQL 업데이트 쿼리 확인해라.
     * 모든 회원의 나이를 1을 더한다.
     */

    @Test
    public void bulkAdd() {
        // 모든 회원의 나이를 1을 더해라.
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();


    }

    @Test
    public void bulkMul() {
        // 모든 회원의 나이를 2 곱해라
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @Test
    public void bulkDelete() {
        // 나이가 10살 이상인 member를 모두 삭제하세요 (bulk 연산) 

        long execute = queryFactory
                .delete(member) // member를 삭제한다.
                .where(member.age.gt(10))
                .execute();


    }


}
