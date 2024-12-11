package cat.tecnocampus.stickeralbum.persistence;

import cat.tecnocampus.stickeralbum.domain.ExchangeOfStickers;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeOfStickersRepository extends JpaRepository<ExchangeOfStickers, Long> {

}
