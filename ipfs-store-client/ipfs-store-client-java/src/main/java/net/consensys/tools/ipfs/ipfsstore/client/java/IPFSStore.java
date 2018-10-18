package net.consensys.tools.ipfs.ipfsstore.client.java;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import lombok.extern.slf4j.Slf4j;
import net.consensys.tools.ipfs.ipfsstore.client.java.exception.IPFSStoreException;
import net.consensys.tools.ipfs.ipfsstore.client.java.model.IdAndHash;
import net.consensys.tools.ipfs.ipfsstore.client.java.model.MetadataAndPayload;
import net.consensys.tools.ipfs.ipfsstore.client.java.wrapper.IPFSStoreWrapper;
import net.consensys.tools.ipfs.ipfsstore.client.java.wrapper.impl.RestIPFSStoreWrapperImpl;
import net.consensys.tools.ipfs.ipfsstore.dto.IndexField;
import net.consensys.tools.ipfs.ipfsstore.dto.IndexerRequest;
import net.consensys.tools.ipfs.ipfsstore.dto.IndexerResponse;
import net.consensys.tools.ipfs.ipfsstore.dto.Metadata;
import net.consensys.tools.ipfs.ipfsstore.dto.query.Query;

/**
 * IPFS Store Java Client
 *
 * @author Gregoire Jeanmart <gregoire.jeanmart@consensys.net>
 */
@Slf4j
public class IPFSStore {

    private static final String ID_ATTRIBUTE = "_id";

    private final IPFSStoreWrapper wrapper;


    /* *********************************************
     * Constructor
     * *********************************************/

    /**
     * Constructor: takes the IPFS-store service endpoint and instantiate the wrapper
     *
     * @param endpoint: IPFS-store service endpoint
     */
    public IPFSStore(String endpoint) {
        this.wrapper = new RestIPFSStoreWrapperImpl(endpoint);
    }

    /**
     * Constructor: takes the IPFS-store service wrapper object
     *
     * @param wrapper
     */
    public IPFSStore(IPFSStoreWrapper wrapper) {
        this.wrapper = wrapper;
    }



    /* *********************************************
     * Public methods
     * *********************************************/

    /**
     * Store a file
     *
     * @param filePath Content (File path)
     * @return IPFS hash
     * @throws IPFSStoreException
     */
    public String store(String filePath) throws IPFSStoreException {

        try(FileInputStream input = new FileInputStream(filePath)) {
            return this.store(input);

        } catch (IOException e) {
            throw new IPFSStoreException(e);
        } 
    }

    /**
     * Store a byte array
     *
     * @param is Content (Input stream)
     * @return IPFS hash
     * @throws IPFSStoreException
     */
    public String store(InputStream is) throws IPFSStoreException {
        try {
            return this.wrapper.store(IOUtils.toByteArray(is));

        } catch (IOException e) {
            throw new IPFSStoreException(e);
        }
    }

    /**
     * Index an IPFS hash with the minimum required arguments.
     * This  funtion indexes an IPFS hash into an index and return an autogenerated id identifying the content in the index
     *
     * @param indexName Index name
     * @param hash      IPFS hash
     * @return IdAndHash  Unique Identifier of the content in the index and IPFS Hash
     * @throws IPFSStoreException
     */
    public IdAndHash index(String indexName, String hash) throws IPFSStoreException {
        return this.index(indexName, hash, null);
    }

    /**
     * Index an IPFS hash
     * This  funtion indexes an IPFS hash into an index with a given ID identifying the content in the index
     *
     * @param indexName Index name
     * @param hash      IPFS hash
     * @param id        Unique identifier of the content in the index
     * @return Unique Identifier of the content in the index
     * @throws IPFSStoreException
     */
    public IdAndHash index(String indexName, String hash, String id) throws IPFSStoreException {
        return this.index(indexName, hash, id, null);
    }

    /**
     * Index an IPFS hash
     * This  funtion indexes an IPFS hash into an index with a given ID identifying the content in the index
     * and the content type (MIMETYPE: application/json, ...)
     *
     * @param indexName   Index name
     * @param hash        IPFS hash
     * @param id          Unique identifier of the content in the index
     * @param contentType Content Type (MIMETYPE)
     * @return IdAndHash  Unique Identifier of the content in the index and IPFS Hash
     * @throws IPFSStoreException
     */
    public IdAndHash index(String indexName, String hash, String id, String contentType) throws IPFSStoreException {
        return this.index(indexName, hash, id, contentType, new ArrayList<>());
    }

    /**
     * Index an IPFS hash
     * This  funtion indexes an IPFS hash into an index with a given ID identifying the content in the index,
     * the content type (MIMETYPE: application/json, ...) and a list of key/value attributes (metadata)
     *
     * @param indexName   Index name
     * @param hash        IPFS hash
     * @param id          Unique identifier of the content in the index
     * @param contentType Content Type (MIMETYPE)
     * @param indexFields Attributes (key/value) to attach to the index document for the given hash and ID
     * @return IdAndHash  Unique Identifier of the content in the index and IPFS Hash
     * @throws IPFSStoreException
     */
    public IdAndHash index(String indexName, String hash, String id, String contentType, Map<String, Object> indexFields)
            throws IPFSStoreException {

        return this.index(indexName, hash, id, contentType, convert(indexFields));
    }

    /**
     * Index an IPFS hash
     * This  funtion indexes an IPFS hash into an index with a given ID identifying the content in the index,
     * the content type (MIMETYPE: application/json, ...) and a list of key/value attributes (metadata)
     *
     * @param indexName   Index name
     * @param hash        IPFS hash
     * @param id          Unique identifier of the content in the index
     * @param contentType Content Type (MIMETYPE)
     * @param indexFields Attributes (key/value) to attach to the index document for the given hash and ID
     * @return IdAndHash  Unique Identifier of the content in the index and IPFS Hash
     * @throws IPFSStoreException
     */
    public IdAndHash index(String indexName, String hash, String id, String contentType, List<IndexField> indexFields)
            throws IPFSStoreException {

        IndexerResponse response = this.wrapper.index(createRequest(indexName, hash, id, contentType, indexFields));
        
        return IdAndHash.builder().hash(hash).id(response.getDocumentId()).build();
    }

    /**
     * Store and Index an IPFS hash with the minimum required arguments.
     * This funtion stores a content into IPFS and then indexes an IPFS hash into an index and return an autogenerated id identifying the content in the index
     *
     * @param file      Content (Input Stream)
     * @param indexName Index name
     * @return Unique Identifier of the content in the index
     * @throws IPFSStoreException
     */
    public IdAndHash index(InputStream file, String indexName) throws IPFSStoreException {
        return this.index(file, indexName, null);
    }

    /**
     * Store and Index an IPFS hash
     * This funtion stores a content into IPFS and then indexes an IPFS hash into an index  with a given ID identifying the content in the index
     *
     * @param file      Content (Input Stream)
     * @param indexName Index name
     * @param id        Unique identifier of the content in the index
     * @return Unique Identifier of the content in the index
     * @throws IPFSStoreException
     */
    public IdAndHash index(InputStream file, String indexName, String id) throws IPFSStoreException {
        return this.index(file, indexName, id, null);
    }

    /**
     * Store and Index an IPFS hash
     * This funtion stores a content into IPFS and then indexes an IPFS hash into an index  with a given ID identifying the content in the index and the content type
     *
     * @param file        Content (Input Stream)
     * @param indexName   Index name
     * @param id          Unique identifier of the content in the index
     * @param contentType Content Type (MIMETYPE)
     * @return Unique Identifier of the content in the index
     * @throws IPFSStoreException
     */
    public IdAndHash index(InputStream file, String indexName, String id, String contentType) throws IPFSStoreException {
        return this.index(file, indexName, id, contentType, new ArrayList<>());
    }

    /**
     * Store and Index an IPFS hash
     * This funtion stores a content into IPFS and then indexes an IPFS hash into an index  with a given ID identifying the content in the index,
     * the content type (MIMETYPE: application/json, ...) and a list of key/value attributes (metadata)
     *
     * @param file        Content (Input Stream)
     * @param indexName   Index name
     * @param id          Unique identifier of the content in the index
     * @param contentType Content Type (MIMETYPE)
     * @param indexFields Attributes (key/value) to attach to the index document for the given hash and ID
     * @return Unique Identifier of the content in the index
     * @throws IPFSStoreException
     */
    public IdAndHash index(InputStream file, String indexName, String id, String contentType,
                        Map<String, Object> indexFields) throws IPFSStoreException {
        return this.index(file, indexName, id, contentType, convert(indexFields));
    }

    /**
     * This funtion stores a content into IPFS and then indexes an IPFS hash into an index  with a given ID identifying the content in the index,
     * the content type (MIMETYPE: application/json, ...) and a list of key/value attributes (metadata)
     *
     * @param file        Content (Input Stream)
     * @param indexName   Index name
     * @param id          Unique identifier of the content in the index
     * @param contentType Content Type (MIMETYPE)
     * @param indexFields Attributes (key/value) to attach to the index document for the given hash and ID
     * @return Unique Identifier of the content in the index
     * @throws IPFSStoreException
     */
    public IdAndHash index(InputStream file, String indexName, String id, String contentType,
                        List<IndexField> indexFields) throws IPFSStoreException {

        try {
            IndexerResponse response = this.wrapper.storeAndIndex(
                    IOUtils.toByteArray(file),
                    createRequest(indexName, null, id, contentType, indexFields)
            );
            
            return IdAndHash.builder().id(response.getDocumentId()).hash(response.getHash()).build();

        } catch (IOException e) {
            throw new IPFSStoreException(e);
        }
    }

    /**
     * Return a document for a given hash
     *
     * @param indexName Index name
     * @param hash      IPFS hash
     * @return Content byte array
     * @throws IPFSStoreException
     */
    public byte[] get(String indexName, String hash) throws IPFSStoreException {
        return this.wrapper.fetch(indexName, hash);
    }

    /**
     * Return a document for a given Index Unique identifier
     *
     * @param indexName Index name
     * @param id        Index document Unique identifier
     * @return Content (metadata + payload)
     * @throws IPFSStoreException
     */
    public MetadataAndPayload getById(String indexName, String id) throws IPFSStoreException {
        Metadata metadata = this.getMetadataById(indexName, id);

        if (metadata != null) {
            return MetadataAndPayload.builder()
                    .metadata(metadata)
                    .payload(this.get(indexName, metadata.getHash()))
                    .build();

        } else {
            return MetadataAndPayload.builder()
                    .metadata(metadata)
                    .payload(new byte[0])
                    .build();
        }
    }

    /**
     * Return the content metadata (index, ID, content_type, hash and attributes)
     *
     * @param indexName Index name
     * @param id        Index document Unique identifier
     * @return Metadata (index, ID, content_type, hash and attributes)
     * @throws IPFSStoreException
     */
    public Metadata getMetadataById(String indexName, String id) throws IPFSStoreException {
        Query query = Query.newQuery().equals(ID_ATTRIBUTE, id);

        Page<Metadata> searchResult = this.wrapper.search(indexName, query, new PageRequest(0, 1));
        if (searchResult.getTotalElements() == 0) {
            log.warn("Content [indexName={}, id={}] not found", indexName, id);
            return null;
        }

        return searchResult.getContent().get(0);
    }

    /**
     * Search all content metadata in the index with default pagination (limit 20)
     *
     * @param indexName Index name
     * @return Page of content metadata (index, ID, content_type, hash and attributes)
     * @throws IPFSStoreException
     */
    public Page<Metadata> search(String indexName) throws IPFSStoreException {
        return this.search(indexName, null);
    }

    /**
     * Search content metadata with search criteria and default pagination (limit 20)
     *
     * @param indexName Index name
     * @param query     Query with search criteria
     * @return Page of content metadata (index, ID, content_type, hash and attributes)
     * @throws IPFSStoreException
     */
    public Page<Metadata> search(String indexName, Query query) throws IPFSStoreException {
        return this.search(indexName, query, null);
    }

    /**
     * Search content metadata with search criteria and pagination
     *
     * @param indexName Index name
     * @param query     Query with search criteria
     * @param pageable  Pagination and Sorting
     * @return Page of content metadata (index, ID, content_type, hash and attributes)
     * @throws IPFSStoreException
     */
    public Page<Metadata> search(String indexName, Query query, Pageable pageable) throws IPFSStoreException {
        return this.wrapper.search(indexName, query, pageable);
    }

    /**
     * Search content metadata with search criteria and pagination
     *
     * @param indexName Index name
     * @param query     Query with search criteria
     * @param pageNo    Page no
     * @param pageSize  Page size
     * @return Page of content metadata (index, ID, content_type, hash and attributes)
     * @throws IPFSStoreException
     */
    public Page<Metadata> search(String indexName, Query query, int pageNo, int pageSize)
            throws IPFSStoreException {
        return this.search(indexName, query, pageNo, pageSize, null, null);
    }

    /**
     * Search content metadata with search criteria, pagination and sorting
     *
     * @param indexName     Index name
     * @param query         Query with search criteria
     * @param pageNo        Page no
     * @param pageSize      Page size
     * @param sortAttribute Sorting attribute
     * @param sortDirection Sorting direction
     * @return Page of content metadata (index, ID, content_type, hash and attributes)
     * @throws IPFSStoreException
     */
    public Page<Metadata> search(String indexName, Query query, int pageNo, int pageSize, String sortAttribute,
                                 Direction sortDirection) throws IPFSStoreException {

        PageRequest pagination;
        if (sortAttribute == null || sortAttribute.isEmpty()) {
            pagination = new PageRequest(pageNo, pageSize);
        } else {
            pagination = new PageRequest(pageNo, pageSize, new Sort(sortDirection, sortAttribute));
        }

        return this.search(indexName, query, pagination);
    }

    /**
     * Search all content with default pagination and returns a content list page
     *
     * @param indexName Index name
     * @return Page of content (metadata + payload)
     * @throws IPFSStoreException
     */
    public Page<MetadataAndPayload> searchAndFetch(String indexName) throws IPFSStoreException {
        return this.searchAndFetch(indexName, null);
    }

    /**
     * Search content with search criteria and with default pagination, and returns a content list page
     *
     * @param indexName Index name
     * @param query     Query with search criteria
     * @return Page of content (metadata + payload)
     * @throws IPFSStoreException
     */
    public Page<MetadataAndPayload> searchAndFetch(String indexName, Query query) throws IPFSStoreException {
        return this.searchAndFetch(indexName, query, null);
    }

    /**
     * Search content with search criteria, pagination and sorting and returns a content list page
     *
     * @param indexName Index name
     * @param query     Query with search criteria
     * @param pageable  Pagination and Sorting
     * @return Page of content (metadata + payload)
     * @throws IPFSStoreException
     */
    public Page<MetadataAndPayload> searchAndFetch(String indexName, Query query, Pageable pageable)
            throws IPFSStoreException {
        Page<Metadata> search = this.wrapper.search(indexName, query, pageable);

        List<MetadataAndPayload> contentList = search.getContent().stream().map(m -> {
            try {
                return MetadataAndPayload.builder()
                        .metadata(m)
                        .payload(this.get(indexName, m.getHash()))
                        .build();
                
            } catch (IPFSStoreException e) {
                log.error("Error while fetching " + m.getHash(), e);
                return null;
            }
        }).collect(Collectors.toList());

        return new PageImpl<>(contentList, pageable, search.getTotalElements());
    }

    /**
     * Search content with search criteria, pagination and returns a content list page
     *
     * @param indexName Index name
     * @param query     Query with search criteria
     * @param pageNo    Page no
     * @param pageSize  Page size
     * @return Page of content (metadata + payload)
     * @throws IPFSStoreException
     */
    public Page<MetadataAndPayload> searchAndFetch(String indexName, Query query, int pageNo, int pageSize)
            throws IPFSStoreException {
        return this.searchAndFetch(indexName, query, pageNo, pageSize, null, null);
    }

    /**
     * Search content with search criteria, pagination and sorting and returns a content list page
     *
     * @param indexName     Index name
     * @param query         Query with search criteria
     * @param pageNo        Page no
     * @param pageSize      Page size
     * @param sortAttribute Sorting attribute
     * @param sortDirection Sorting direction
     * @return Page of content (metadata + payload)
     * @throws IPFSStoreException
     */
    public Page<MetadataAndPayload> searchAndFetch(String indexName, Query query, int pageNo, int pageSize, String sortAttribute,
                                       Direction sortDirection) throws IPFSStoreException {

        PageRequest pagination;
        if (sortAttribute == null || sortAttribute.isEmpty()) {
            pagination = new PageRequest(pageNo, pageSize);
        } else {
            pagination = new PageRequest(pageNo, pageSize, new Sort(sortDirection, sortAttribute));
        }

        return this.searchAndFetch(indexName, query, pagination);
    }

    /**
     * Create a new index
     *
     * @param index Index name
     * @throws IPFSStoreException
     */
    public void createIndex(String index) throws IPFSStoreException {
        this.wrapper.createIndex(index);
    }

    /**
     * Return the wrapper
     *
     * @return wrapper
     */
    public IPFSStoreWrapper getWrapper() {
        return this.wrapper;
    }




    /* *********************************************
     * Private methods
     * *********************************************/

    /**
     * Convert a map<String, Object> to a list of IndexField (Key/Value pair)
     *
     * @param indexFields Map of data that need to be indexed against the document
     * @return A converted Map into a list of IndexField (Key/Value pair)
     */
    private static List<IndexField> convert(Map<String, Object> indexFields) {
        return indexFields.entrySet()
                .stream()
                .map(e -> new IndexField(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Factory for a IndexerRequest
     *
     * @param indexName   Index name
     * @param hash        IPFS hash
     * @param id          Document ID
     * @param contentType Content Type
     * @param indexFields Index fields (key/value attribute)
     * @return IndexerRequest object
     */
    private static IndexerRequest createRequest(String index, String hash, String id, String contentType, List<IndexField> indexFields) {
        IndexerRequest request = new IndexerRequest();
        request.setIndex(index);
        request.setHash(hash);
        request.setDocumentId(id);
        request.setContentType(contentType);
        request.setIndexFields(indexFields);

        return request;
    }

}
