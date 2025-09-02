import enum
from datetime import datetime

from db.connect import Base
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import String, Date, Boolean, Integer, LargeBinary, ForeignKey, DateTime, Enum


class DbMessage(Base):
    __tablename__ = "messages"

    message_id: Mapped[int] = mapped_column(Integer(), primary_key=True, index=True)
    sender_id: Mapped[int] = mapped_column(Integer(), ForeignKey("users.user_id"), nullable=False)
    receiver_id: Mapped[int] = mapped_column(Integer(), ForeignKey("users.user_id"), nullable=False)
    chat_id: Mapped[int] = mapped_column(Integer(), ForeignKey("chats.chat_id"), nullable=False)
    message: Mapped[str] = mapped_column(String(255), nullable=False)
    timestamp: Mapped[datetime] = mapped_column(DateTime(), nullable=False)
    file_id: Mapped[int | None] = mapped_column(Integer(), ForeignKey("files.file_id"), nullable=True)
    read: Mapped[bool] = mapped_column(Boolean, nullable=False)
    read_at: Mapped[datetime | None] = mapped_column(DateTime(), nullable=True)
    deleted: Mapped[bool] = mapped_column(Boolean(), nullable=False)
    deleted_by: Mapped[int | None] = mapped_column(Integer, ForeignKey("users.user_id"), nullable=True)
    edited: Mapped[bool] = mapped_column(Boolean(), nullable=False, default=False)
    edited_at: Mapped[datetime | None] = mapped_column(DateTime(), nullable=True)

    file: Mapped["DbFile"] = relationship("DbFile", lazy="raise")
    sender: Mapped["DbUser"] = relationship("DbUser", foreign_keys=[sender_id], lazy="raise")
    receiver: Mapped["DbUser"] = relationship("DbUser", foreign_keys=[receiver_id], lazy="raise")
    chat: Mapped["DbChat"] = relationship("DbChat", lazy="raise")


class DbUser(Base):
    __tablename__ = "users"

    user_id: Mapped[int] = mapped_column(Integer(), primary_key=True, index=True)
    username: Mapped[str] = mapped_column(String(50), nullable=False, unique=True)
    hashed_password: Mapped[str] = mapped_column(String(), nullable=False)
    is_disabled: Mapped[bool] = mapped_column(Boolean(), nullable=False)
    avatar_file_id: Mapped[int | None] = mapped_column(Integer(), nullable=True)
    last_seen: Mapped[datetime] = mapped_column(DateTime(), nullable=False)
    is_admin: Mapped[bool | None] = mapped_column(Boolean(), nullable=False)

    sent_messages: Mapped[list["DbMessage"]] = relationship("DbMessage", foreign_keys="DbMessage.sender_id",
                                                            back_populates="sender", lazy="raise")
    received_messages: Mapped[list["DbMessage"]] = relationship("DbMessage", foreign_keys="DbMessage.receiver_id",
                                                        back_populates="receiver", lazy="raise")
    files: Mapped[list["DbFile"]] = relationship("DbFile", back_populates="user", lazy="raise")
    memberships: Mapped[list["DbChatMember"]] = relationship("DbChatMember", back_populates="user")


class DbFile(Base):
    __tablename__ = "files"

    file_id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column( Integer(), ForeignKey("users.user_id"), nullable=False)
    uploaded_at: Mapped[datetime] = mapped_column(DateTime(), nullable=False)
    filename: Mapped[str] = mapped_column(String(), nullable=False)
    content_type: Mapped[str] = mapped_column(String(), nullable=False)
    data: Mapped[bytes] = mapped_column(LargeBinary(), nullable=False)

    user: Mapped["DbUser"] = relationship("DbUser", lazy="raise")


class DbChat(Base):
    __tablename__ = "chats"

    chat_id: Mapped[int] = mapped_column(Integer(), primary_key=True, index=True)
    chat_name: Mapped[str | None] = mapped_column(String(), nullable=True)
    chat_avatar_file_id: Mapped[int | None] = mapped_column(ForeignKey("files.file_id"), nullable=True)
    is_group: Mapped[bool] = mapped_column(Boolean(), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(), nullable=False)

    members: Mapped[list["DbChatMember"]] = relationship("DbChatMember", back_populates="chat")

class MemberRole(enum.Enum):
    member = "member"
    admin = "admin"
    owner = "owner"

class DbChatMember(Base):
    __tablename__ = "chat_members"

    chat_id: Mapped[int] = mapped_column(ForeignKey("chats.chat_id"), primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.user_id"), primary_key=True)
    role: Mapped[MemberRole] = mapped_column(Enum(MemberRole), nullable=False, default=MemberRole.member)

    chat: Mapped["DbChat"] = relationship("DbChat", back_populates="members")
    user: Mapped["DbUser"] = relationship("DbUser", back_populates="memberships")


    
