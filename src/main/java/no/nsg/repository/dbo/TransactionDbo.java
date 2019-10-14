package no.nsg.repository.dbo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import no.nsg.repository.TransformationManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;


@JsonIgnoreProperties({"id"}) /* Default serialization insists on appending this lowercase id element?!? We do not want it */
public class TransactionDbo {
    public static final int UNINITIALIZED = 0;

    public static final int NO_DIRECTION = 0;
    public static final int OUTBOUND_DIRECTION = 1;
    public static final int INBOUND_DIRECTION = 2;


    @JsonIgnore
    private int _id;

    @JsonIgnore
    private int _id_transactionset;

    private String transactionid;

    @JsonIgnore
    private LocalDateTime transactionTime;

    @JsonIgnore
    private TransformationManager.Direction direction = TransformationManager.Direction.DOESNT_MATTER;


    public TransactionDbo() {
        this(null);
    }

    public TransactionDbo(final TransactionSetDbo transactionSetDbo) {
        this._id = UNINITIALIZED;
        generateTransactionid();
        set_TransactionSetId(transactionSetDbo == null ? TransactionSetDbo.UNINITIALIZED : transactionSetDbo.get_id());
    }

    public TransactionDbo(final Connection connection, final int id) throws SQLException {
        if (id == UNINITIALIZED) {
            throw new NoSuchElementException();
        }

        final String sql = "SELECT _id_transactionset, transactionid, transactiontime, direction FROM nsg.transaction WHERE _id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new NoSuchElementException();
            }

            this._id = id;

            set_TransactionSetId(rs.getInt("_id_transactionset"));

            setTransactionid(rs.getString("transactionid"));

            Timestamp tmpTime = rs.getTimestamp("transactiontime");
            if (!rs.wasNull()) {
                setTransactionTime(tmpTime.toLocalDateTime());
            } else {
                setTransactionTime(null);
            }

            Integer tmpDirection;
            tmpDirection = rs.getInt("direction");
            if (rs.wasNull()) {
                tmpDirection = null;
            }
            setDirection(intToDirection(tmpDirection));

        }
    }

    public static TransactionDbo create(final Connection connection, final String orgnr, final String transactionSetName) throws SQLException {
        CompanyDbo companyDbo = CompanyDbo.getOrCreateByOrgno(connection, orgnr);
        TransactionSetDbo transactionSetDbo = TransactionSetDbo.getOrCreateByCompanyAndName(connection, companyDbo, transactionSetName);
        TransactionDbo transactionDbo = new TransactionDbo(transactionSetDbo);
        transactionDbo.persist(connection);
        return transactionDbo;
    }

    public int get_id() {
        return this._id;
    }

    private void set_TransactionSetId(final int _transactionSetId) {
        this._id_transactionset = _transactionSetId;
    }

    private int get_TransactionSetId() {
        return this._id_transactionset;
    }

    private void setTransactionid(final String transactionid) {
        this.transactionid = transactionid;
    }

    private void generateTransactionid() {
        if (getTransactionid() == null) {
            this.transactionid = UUID.randomUUID().toString();
        }
    }

    public String getTransactionid() {
        return transactionid;
    }

    public void setTransactionTime(final LocalDateTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public LocalDateTime getTransactionTime() {
        return this.transactionTime;
    }

    public void setDirection(final TransformationManager.Direction direction) {
        this.direction = direction;
    }

    public TransformationManager.Direction getDirection() {
        return direction;
    }

    public void persist(final Connection connection) throws SQLException {
        if (get_id() == UNINITIALIZED) {
            final String sql = "INSERT INTO nsg.transaction (_id_transactionset, transactionid, transactiontime, direction) VALUES (?,?,?,?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                if (get_TransactionSetId() == TransactionSetDbo.UNINITIALIZED) {
                    stmt.setNull(1, Types.INTEGER);
                } else {
                    stmt.setInt(1, get_TransactionSetId());
                }

                stmt.setString(2, getTransactionid());

                if (getTransactionTime() == null) {
                    stmt.setNull(3, Types.TIMESTAMP);
                } else {
                    stmt.setTimestamp(3, Timestamp.valueOf(getTransactionTime()));
                }

                Integer tmpDirection = directionToInt(getDirection());
                if (tmpDirection == null) {
                    stmt.setNull(4, Types.INTEGER);
                } else {
                    stmt.setInt(4, tmpDirection);
                }

                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    this._id = rs.getInt(1);
                }
            }
        } else {
            final String sql = "UPDATE nsg.transaction SET _id_transactionset=?, transactionid=?, transactiontime=?, direction=? WHERE _id=?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                if (get_TransactionSetId() == TransactionSetDbo.UNINITIALIZED) {
                    stmt.setNull(1, Types.INTEGER);
                } else {
                    stmt.setInt(1, get_TransactionSetId());
                }

                stmt.setString(2, getTransactionid());

                if (getTransactionTime() == null) {
                    stmt.setNull(3, Types.TIMESTAMP);
                } else {
                    stmt.setTimestamp(3, Timestamp.valueOf(getTransactionTime()));
                }

                Integer tmpDirection = directionToInt(getDirection());
                if (tmpDirection == null) {
                    stmt.setNull(4, Types.INTEGER);
                } else {
                    stmt.setInt(4, tmpDirection);
                }

                stmt.setInt(5, get_id());

                stmt.executeUpdate();
            }
        }
    }

    private TransformationManager.Direction intToDirection(final Integer direction) {
        if (direction == null) {
            return null;
        }

        switch(direction) {
            case INBOUND_DIRECTION: return TransformationManager.Direction.PURCHASE;
            case OUTBOUND_DIRECTION: return TransformationManager.Direction.SALES;
            default: return TransformationManager.Direction.DOESNT_MATTER;
        }
    }

    private Integer directionToInt(final TransformationManager.Direction direction) {
        if (direction == null) {
            return null;
        }

        switch(direction) {
            case PURCHASE: return INBOUND_DIRECTION;
            case SALES: return OUTBOUND_DIRECTION;
            default: return NO_DIRECTION;
        }
    }

}
