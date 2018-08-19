package oliweb.nc.oliweb.dto.elasticsearch;

/**
 * Created by orlanth23 on 23/02/2018.
 */

public class ElasticsearchResult<T> {
    private String _id;
    private String _index;
    private Float _score;
    private String _type;
    private T _source;

    public ElasticsearchResult() {
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String get_index() {
        return _index;
    }

    public void set_index(String _index) {
        this._index = _index;
    }

    public Float get_score() {
        return _score;
    }

    public void set_score(Float _score) {
        this._score = _score;
    }

    public String get_type() {
        return _type;
    }

    public void set_type(String _type) {
        this._type = _type;
    }

    public T get_source() {
        return _source;
    }

    public void set_source(T _source) {
        this._source = _source;
    }
}
