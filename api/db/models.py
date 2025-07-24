from datetime import datetime

from api.db.connect import Base
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import String, Date, Boolean, Integer, LargeBinary, ForeignKey


class DbMessage(Base):
    __tablename__ = "messages"

    message_id: Mapped[int] = mapped_column(Integer(), primary_key=True, index=True)
    sender_id: Mapped[int] = mapped_column(Integer(), ForeignKey("users.user_id"), nullable=False)
    receiver_id: Mapped[int] = mapped_column(Integer(), ForeignKey("users.user_id"), nullable=False)
    message: Mapped[str] = mapped_column(String(255), nullable=False)
    timestamp: Mapped[datetime] = mapped_column(Date(), nullable=False)
    file_id: Mapped[int] = mapped_column(Integer(), ForeignKey("files.file_id"), nullable=True)
    read: Mapped[bool] = mapped_column(Boolean, nullable=False)
    read_at: Mapped[datetime] = mapped_column(Date(), nullable=True)
    deleted: Mapped[bool] = mapped_column(Boolean(), nullable=False)

    file: Mapped["DbFile"] = relationship("DbFile", lazy="raise")
    sender: Mapped["DbUser"] = relationship("DbUser", foreign_keys=[sender_id], lazy="raise")
    receiver: Mapped["DbUser"] = relationship("DbUser", foreign_keys=[receiver_id], lazy="raise")


class DbUser(Base):
    __tablename__ = "users"

    user_id: Mapped[int] = mapped_column(Integer(), primary_key=True, index=True)
    username: Mapped[str] = mapped_column(String(50), nullable=False, unique=True)
    hashed_password: Mapped[str] = mapped_column(String(), nullable=False)
    is_disabled: Mapped[bool] = mapped_column(Boolean(), nullable=False)

    sent_messages: Mapped[list["DbMessage"]] = relationship("DbMessage", foreign_keys="DbMessage.sender_id",
                                                            back_populates="sender", lazy="raise")
    received_messages: Mapped[list["DbMessage"]] = relationship("DbMessage", foreign_keys="DbMessage.receiver_id",
                                                        back_populates="receiver", lazy="raise")
    files: Mapped[list["DbFile"]] = relationship("DbFile", back_populates="user", lazy="raise")

class DbFile(Base):
    __tablename__ = "files"

    file_id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column( Integer(), ForeignKey("users.user_id"), nullable=False)
    uploaded_at: Mapped[datetime] = mapped_column(Date(), nullable=False)
    filename: Mapped[str] = mapped_column(String, nullable=False)
    content_type: Mapped[str] = mapped_column(String, nullable=False)
    data: Mapped[bytes] = mapped_column(LargeBinary, nullable=False)

    user: Mapped["DbUser"] = relationship("DbUser", lazy="raise")
