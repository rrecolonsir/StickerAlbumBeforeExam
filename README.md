# Exam course 2024-2025

This will be (with few changes) the base code of the exam. Please, get familiar with it before the exam.

## The domain: Albums, Stickers, Collections and Auctions
This domain is familiar to you, and it has advantages and disadvantages at the same time. It is an advantage because you know the domain and what to expect, but it could also be a disadvantage because
it may be implemented differently than your own project. Please, read the code carefully and understand the domain before you start coding.

+ Users are called *Collectors*
+ There are *Albums*, *Sections* and *Stickers*. I believe this code is similar to yours.
+ There are *Collections* that consist of a set of stickers that a collector has of a given album.
```java
@Entity
public class Collection {

    @EmbeddedId
    CollectionPK collectionPK;

    @ManyToOne
    @JoinColumn(name="collector_id", referencedColumnName="id")
    @MapsId("collector_id")
    private Collector collector;

    @ManyToOne
    @JoinColumn(name="album_id", referencedColumnName="id")
    @MapsId("album_id")
    private Album album;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "collection")
    List<HasSticker> ownedStickers;

    //and more
```
The primary key is embedded in the class CollectionPK and contains the id of the collector and the Album. The Collection also has a Collector and an Album as attributes.
For the attributes *collector_id* and *album_id* not to be repeated in the database table we use the @MapsId annotation. This is a JPA annotation that tells the JPA provider to use the id of the Album and the Collector as the id of the Collection.

+ A *Collection* has a list of *HasSticker*. A *HasSticker* is a class that represents a sticker that a collector has in his collection with the number of copies.
```java
@Entity
public class HasSticker {

    @EmbeddedId
    HasStickerPK hasStickerPK;

    @ManyToOne
    @JoinColumn(name="sticker_id", referencedColumnName="id")
    @MapsId("sticker_id")
    private Sticker sticker;

    @ManyToOne
    @JoinColumns({
            @JoinColumn(name="collector_id", referencedColumnName="collector_id", insertable = false, updatable = false),
            @JoinColumn(name="album_id", referencedColumnName="album_id", insertable = false, updatable = false)
    })
    private Collection collection;
    
    // and more
```
The primary key is embedded in the class HasStickerPK and contains the id of the sticker and the id of the collection (that in turn is a composed primary key).
The HasSticker also has a Sticker and a Collection. The @MapsId annotation is used to avoid repeating the *sticker_id* in the database table, and the
@JoinColumns annotation is used to avoid repeating the *collector_id* and *album_id* in the database table.

+ The association between *Collection* and *HasSticker* is a @OneToMany relationship. Following the Vlad Mihaela's advice [@OneToMany](https://vladmihalcea.com/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/), we implemented it with a
  bidirectional association in order to avoid extra updates when adding and deleting HasStickers in to a Collection. Note that, in JPA, the owner of the relationship
  is the entity that contains the foreign key. In this case, the HasSticker entity contains the foreign key references to both the Collection and Sticker entities.
  The Collection entity has a List<HasSticker> with the mappedBy attribute, indicating that the HasSticker entity owns the relationship. It also indicates that when
  HasStickers are added, modified, or deleted in its list; the HasSticker must be inserted, updated, or deleted in the database.

  The @JoinColumns annotation in the HasSticker entity is necessary because the HasSticker entity has a composite primary key that includes foreign keys from both the Collection
  and Sticker entities. The Collection entity itself has a composite primary key consisting of collector_id and album_id. Therefore, to establish the relationship
  between HasSticker and Collection, we must specify collector_id and album_id as foreign keys. It also indicates that these columns are not insertable or updatable,
  because they are part of the primary key of the HasSticker entity. It tells not to repeat the attributes in the database table.

  One consequence of *Collection* having a *List<HasSticker>* is that the logic of collecting stickers is in the Collection entity. In fact, the *Collection*
  does not have any public method that needs as input or returns a HasSticker.

+ The **ExchangeOfStickers** is a class representing an exchange of stickers proposal from one collector (origin) to another (destination). The proposal
  can be accepted or rejected by the destination user. The exchange must be from stickers of the same Album. The constructor of the ExchangeOfStickers calculates
  the maximum number of stickers that can be exchanged, and the actual stickers to exchange. Note that the class *Collection* has a method that calculates the stickers that could be exchanged.
  Both classes use the *ExchangeableStickers* record, which contains stickers that can be exchanged between two collections.

+ In this application, one collector, can make an **auction (*BlindAuction*)** of a sticker he has. The auction has a starting date, an end date, and an initial price. Other collectors can bid for the sticker.
  A bid must offer a price equal to or higher than the current price of the auction. One can bid only when the auction is open (between the starting and end date).
  The auction is called blind because one bids without knowing the other bids. In this case, the association between *BlindAuction* and *Bid* is a @ManyToOne relationship
  because an auction could have thousands of bids. One consequence of the auction not having a list of bids is that most of the logic is in the service layer. 

