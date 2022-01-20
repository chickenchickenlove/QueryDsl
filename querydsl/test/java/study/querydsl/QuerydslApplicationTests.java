package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@Commit // 커밋까지 해준다. 롤백 아니고.
class QuerydslApplicationTests {

	@Autowired
	EntityManager em;


	@Test
	void contextLoads() {

		Hello hello = new Hello();
		em.persist(hello);

		JPAQueryFactory query = new JPAQueryFactory(em); // 엔티티 매니저를 넣는다.
//		QHello qHello = new QHello("h"); // 앨리어스를 h로 넣었다.
		QHello qHello = QHello.hello; // 이렇게 쓰는 방법이 있다. 이미 QType에서 스태틱으로 만들어 둠.


		//query dsl에서 쓸려면, 쿼리와 관련된 것은 모두 QType을 써야한다.

		Hello result = query
				.selectFrom(qHello) // 방금 만든 qHello 넣어준다.
				.fetchOne();

		assertThat(result).isEqualTo(hello);
		assertThat(result.getId()).isEqualTo(hello.getId());
	}




}
