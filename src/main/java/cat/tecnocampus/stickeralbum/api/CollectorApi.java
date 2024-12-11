package cat.tecnocampus.stickeralbum.api;

import cat.tecnocampus.stickeralbum.application.AlbumService;
import cat.tecnocampus.stickeralbum.application.CollectionService;
import cat.tecnocampus.stickeralbum.application.inputDTOs.AddStickerCommand;
import cat.tecnocampus.stickeralbum.application.inputDTOs.AlbumCommand;
import cat.tecnocampus.stickeralbum.application.inputDTOs.CollectionCommand;
import cat.tecnocampus.stickeralbum.application.inputDTOs.ExchangeStickersCommand;
import cat.tecnocampus.stickeralbum.application.outputDTOs.AlbumDTO;
import cat.tecnocampus.stickeralbum.application.outputDTOs.CollectionDTO;
import cat.tecnocampus.stickeralbum.application.outputDTOs.ExchangeableStickersDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CollectorApi {

    private final AlbumService albumService;
    private final CollectionService collectionService;

    public CollectorApi(AlbumService albumService, CollectionService collectionService) {
        this.albumService = albumService;
        this.collectionService = collectionService;
    }

    @PostMapping("/albums")
    @ResponseStatus(HttpStatus.CREATED)
    public AlbumCommand createAlbum(@RequestBody AlbumCommand album) {
        albumService.createAlbum(album);
        return album;
    }

    @GetMapping("/albums/{albumId}")
    public AlbumDTO getAlbum(@PathVariable Long albumId) {
        return albumService.getAlbum(albumId);
    }

    @PostMapping("/collections")
    @ResponseStatus(HttpStatus.CREATED)
    public void createCollection(@RequestBody CollectionCommand collection) {
        collectionService.createCollection(collection);
    }

    @PostMapping("/collectors/stickers")
    @ResponseStatus(HttpStatus.CREATED)
    public void addStickerQuantityToCollection(@RequestBody AddStickerCommand addStickerCommand) {
        collectionService.addStickerQuantityToCollection(addStickerCommand.collectorId(), addStickerCommand.albumId(),
                addStickerCommand.stickerNumber(), addStickerCommand.numberOfCopies());
    }

    @GetMapping("/collectors/{collectorId}/collections")
    public List<CollectionDTO> getCollections(@PathVariable Long collectorId) {
        return collectionService.getCollections(collectorId);
    }

    @GetMapping("/collectors/{collectorId}/collections/open")
    public List<CollectionDTO> getOpenCollections(@PathVariable Long collectorId) {
        return collectionService.getOpenCollections(collectorId);
    }

    @PostMapping("/collections/singleExchange")
    @ResponseStatus(HttpStatus.CREATED)
    public void singleExchange(@RequestBody ExchangeStickersCommand exchangeStickersCommand) {
        collectionService.singleExchange(exchangeStickersCommand);
    }

    @GetMapping("/collectors/{collectorId1}/collector/{collectorId2}/albums/{albumId}/exchangeableStickers")
    public ExchangeableStickersDTO getExchangeableStickers(@PathVariable Long collectorId1, @PathVariable Long collectorId2, @PathVariable Long albumId) {
        return collectionService.getExchangeableStickers(collectorId1, collectorId2, albumId);
    }

    @PostMapping("/collectors/{collectorId1}/collector/{collectorId2}/albums/{albumId}/exchangeProposals")
    public ResponseEntity<Void> makeExchangeProposal(@PathVariable Long collectorId1, @PathVariable Long collectorId2, @PathVariable Long albumId) {
        boolean proposalMade = collectionService.exchangeProposal(collectorId1, collectorId2, albumId);
        return proposalMade ? ResponseEntity.status(HttpStatus.CREATED).build() : ResponseEntity.ok().build();
    }

    @PutMapping("/collectors/{collectorId}/exchangeProposals/{exchangeId}/status")
    @ResponseStatus(HttpStatus.OK)
    public void updateExchangeStatus(@PathVariable Long collectorId, @PathVariable Long exchangeId, @RequestParam String status) {
        if ("accepted".equalsIgnoreCase(status)) {
            collectionService.acceptExchange(collectorId, exchangeId);
        } else if ("rejected".equalsIgnoreCase(status)) {
            collectionService.rejectExchange(collectorId, exchangeId);
        } else {
            throw new IllegalArgumentException("Invalid status");
        }
    }
}
