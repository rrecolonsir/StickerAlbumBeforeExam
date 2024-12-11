package cat.tecnocampus.stickeralbum.application;

import cat.tecnocampus.stickeralbum.application.exceptions.*;
import cat.tecnocampus.stickeralbum.application.inputDTOs.CollectionCommand;
import cat.tecnocampus.stickeralbum.application.inputDTOs.ExchangeStickersCommand;
import cat.tecnocampus.stickeralbum.application.mapper.ExchangeableStickerMapper;
import cat.tecnocampus.stickeralbum.application.outputDTOs.CollectionDTO;
import cat.tecnocampus.stickeralbum.application.outputDTOs.ExchangeableStickersDTO;
import cat.tecnocampus.stickeralbum.domain.*;
import cat.tecnocampus.stickeralbum.persistence.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CollectionService {
    private final CollectorRepository collectorRepository;
    private final CollectionRepository collectionRepository;
    private final HasStickerRepository hasStickerRepository;
    private final AlbumRepository albumRepository;
    private final StickerRepository stickerRepository;
    private final ExchangeOfStickersRepository exchangeOfStickersRepository;

    public CollectionService(CollectorRepository collectorRepository, CollectionRepository collectionRepository,
                             HasStickerRepository hasStickerRepository, AlbumRepository albumRepository, StickerRepository stickerRepository,
                             ExchangeOfStickersRepository exchangeOfStickersRepository) {
        this.collectorRepository = collectorRepository;
        this.collectionRepository = collectionRepository;
        this.hasStickerRepository = hasStickerRepository;
        this.albumRepository = albumRepository;
        this.stickerRepository = stickerRepository;
        this.exchangeOfStickersRepository = exchangeOfStickersRepository;
    }

    public void createCollection(CollectionCommand collection) {
        Collector collector = collectorRepository.findById(collection.collectorId())
                .orElseThrow(() -> new CollectorDoesNotExistException(collection.collectorId()));
        Album album = albumRepository.findById(collection.albumId())
                .orElseThrow(() -> new AlbumDoesNotExistException(collection.albumId()));
        Collection domainCollection = new Collection(collector, album, collection.beginDate(), collection.endDate());
        collectionRepository.save(domainCollection);
    }

    @Transactional
    public void addStickerQuantityToCollection(Long collectorId, Long albumId, Long stickerNUmber, int quantity) {
        Collector collector = collectorRepository.findById(collectorId)
                .orElseThrow(() -> new CollectorDoesNotExistException(collectorId));
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new AlbumDoesNotExistException(albumId));
        if (!album.isActive()) throw new AlbumNotActiveException(albumId);
        Collection collection = collectionRepository.findById(new CollectionPK(collector.getId(),album.getId()))
                .orElseThrow(() -> new CollectionDoesNotExistException(collector.getId(),album.getId()));
        Sticker sticker = album.getStickerWithNumber(stickerNUmber)
                .orElseThrow(() -> new StickerDoesNotExistException(stickerNUmber));
        collection.addSticker(sticker, quantity);
    }

    public List<CollectionDTO> getCollections(Long collectorId) {
        collectorRepository.findById(collectorId).orElseThrow(() -> new CollectorDoesNotExistException(collectorId));
        return collectionRepository.findAllByCollectorId(collectorId);
    }

    public List<CollectionDTO> getOpenCollections(Long collectorId) {
        collectorRepository.findById(collectorId).orElseThrow(() -> new CollectorDoesNotExistException(collectorId));
        return collectionRepository.findOpenByCollectionPKCollectorId(collectorId);
    }

    @Transactional
    public void singleExchange(ExchangeStickersCommand exchangeStickersCommand) {
        var collector1 = collectorRepository.findById(exchangeStickersCommand.collectorId1())
                .orElseThrow(() -> new CollectorDoesNotExistException(exchangeStickersCommand.collectorId1()));
        var collector2 = collectorRepository.findById(exchangeStickersCommand.collectorId2())
                .orElseThrow(() -> new CollectorDoesNotExistException(exchangeStickersCommand.collectorId2()));
        var sticker1 = stickerRepository.findById(exchangeStickersCommand.stickerId1())
                .orElseThrow(() -> new StickerDoesNotExistException(exchangeStickersCommand.stickerId1()));
        var sticker2 = stickerRepository.findById(exchangeStickersCommand.stickerId2())
                .orElseThrow(() -> new StickerDoesNotExistException(exchangeStickersCommand.stickerId2()));
        var collection1 = collectionRepository.findByCollectorAndSticker(collector1, sticker1)
                .orElseThrow(() -> new StickerNotInCollectionException(sticker1.getId(), collector1.getId()));
        var collection2 = collectionRepository.findByCollectorAndSticker(collector2, sticker2)
                .orElseThrow(() -> new StickerNotInCollectionException(sticker1.getId(), collector2.getId()));

        if (collection1.getAlbum() != collection2.getAlbum())
            throw new IllegalStateException("Stickers are not from the same album");

        collection1.blockSticker(sticker1);
        collection2.blockSticker(sticker2);

        collection1.removeBlockedSticker(sticker1);
        collection2.removeBlockedSticker(sticker2);

        collection1.addSticker(sticker2, 1);
        collection2.addSticker(sticker1, 1);
    }

    public ExchangeableStickersDTO getExchangeableStickers(Long ownerId1, Long ownerId2, Long albumId) {
        Collector collector1 = collectorRepository.findById(ownerId1).orElseThrow();
        Collector collector2 = collectorRepository.findById(ownerId2).orElseThrow();
        Album album = albumRepository.findById(albumId).orElseThrow();
        var origin = collectionRepository.findByCollectorAndAlbum(collector1, album).orElseThrow();
        var destination = collectionRepository.findByCollectorAndAlbum(collector2, album).orElseThrow();
        return ExchangeableStickerMapper.mapExchangeableStickersDTO(origin.getExchangeableStickers(destination));
    }

    // Returns true if the exchange is possible (there are stickers to exchange), false otherwise
    public boolean exchangeProposal(Long ownerId1, Long ownerId2, Long albumId) {
        Collector collector1 = collectorRepository.findById(ownerId1).orElseThrow(() -> new CollectorDoesNotExistException(ownerId1));
        Collector collector2 = collectorRepository.findById(ownerId2).orElseThrow(() -> new CollectorDoesNotExistException(ownerId2));
        Album album = albumRepository.findById(albumId).orElseThrow(() -> new AlbumDoesNotExistException(albumId));
        var origin = collectionRepository.findByCollectorAndAlbum(collector1, album)
                .orElseThrow(() -> new CollectionDoesNotExistException(albumId, ownerId1));
        var destination = collectionRepository.findByCollectorAndAlbum(collector2, album)
                .orElseThrow(() -> new CollectionDoesNotExistException(albumId, ownerId2));
        var exchangeProposal = new ExchangeOfStickers(origin, destination);
        if (exchangeProposal.isPossible()) exchangeOfStickersRepository.save(exchangeProposal);
        return exchangeProposal.isPossible();
    }

    @Transactional
    public void acceptExchange(Long destinationUserId, Long exchangeId) {
        getExchange(destinationUserId, exchangeId).acceptExchange();
    }

    @Transactional
    public void rejectExchange(Long destinationUserId, Long exchangeId) {
        getExchange(destinationUserId, exchangeId).rejectExchange();
    }

    private ExchangeOfStickers getExchange(Long destinationUserId, Long exchangeId) {
        var destinationCollector = collectorRepository.findById(destinationUserId).orElseThrow(() -> new CollectorDoesNotExistException(destinationUserId));
        ExchangeOfStickers exchange = exchangeOfStickersRepository.findById(exchangeId).orElseThrow(() -> new ExchangeDoesNotExistException(exchangeId));
        if (exchange.getStatus() != ExchangeStatus.PENDING) throw new RuntimeException("Exchange not pending");
        if (exchange.getDestinationUser() != destinationCollector) throw new RuntimeException("Not the destination collector");
        return exchange;
    }
}
