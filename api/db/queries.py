import asyncio
from datetime import datetime
from fastapi import UploadFile
from sqlalchemy import select, or_, and_, update, func
from sqlalchemy.exc import IntegrityError, DataError

from schemas.chat_schema import Chat
from schemas.file_schema import MAX_FILE_SIZE
from schemas.message_schema import MessageInDb, Message
from db.connect import async_session_factory, Base, engine
from db.models import DbMessage, DbUser, DbFile, DbChat, DbChatMember
from schemas.user_schemas import UserInDb



class ORM:
    @staticmethod
    async def create_tables():
        async with engine.begin() as conn:
            engine.echo = True
            await conn.run_sync(Base.metadata.drop_all)
            await conn.run_sync(Base.metadata.create_all)

    @staticmethod
    async def register_user(user: dict):
        async with async_session_factory() as session:
            obj = DbUser(**user)
            try:
                session.add(obj)
                await session.flush()
                await session.refresh(obj)
                pydantic_model = UserInDb.model_validate(obj)
                await session.commit()
                return pydantic_model
            except IntegrityError as e:
                session.rollback()
                raise ValueError(f"Ошибка целостности данных: {str(e)}")
            except DataError as e:
                await session.rollback()
                raise ValueError(f"Ошибка типа данных: {str(e)}")
            except Exception as e:
                await session.rollback()
                raise ValueError(f"Неожиданная ошибка: {str(e)}")

    @staticmethod
    async def get_chat_member(chat_id, user_id):
        async with async_session_factory() as session:
            try:
                member_id = await session.scalar(
                    select(DbChatMember.user_id).where(DbChatMember.chat_id == chat_id, DbChatMember.user_id != user_id)
                )
                return member_id
            except IntegrityError as e:
                raise ValueError(f"Ошибка целостности данных: {str(e)}")
            except DataError as e:
                raise ValueError(f"Ошибка типа данных: {str(e)}")
            except Exception as e:
                raise ValueError(f"Неожиданная ошибка: {str(e)}")

    @staticmethod
    async def send_message(message: Message) -> MessageInDb:
        async with async_session_factory() as session:

            obj = DbMessage(**message.model_dump())
            try:
                session.add(obj)
                await session.flush()
                await session.refresh(obj)
                pydantic_model = MessageInDb.model_validate(obj)
                await session.commit()
                return pydantic_model
            except IntegrityError as e:
                session.rollback()
                raise ValueError(f"Ошибка целостности данных: {str(e)}")
            except DataError as e:
                await session.rollback()
                raise ValueError(f"Ошибка типа данных: {str(e)}")
            except Exception as e:
                await session.rollback()
                raise ValueError(f"Неожиданная ошибка: {str(e)}")

    @staticmethod
    async def get_chat(sender_id, chat_id) -> list[MessageInDb]:
        async with async_session_factory() as session:
            try:
                is_member = await session.scalar(
                    select(func.count())
                    .select_from(DbChatMember)
                    .where(
                        DbChatMember.chat_id == chat_id,
                        DbChatMember.user_id == sender_id
                    )
                )
                if not is_member:
                    raise PermissionError("Пользователь не является участником этого чата")

                stmt = (
                    select(DbMessage)
                    .where(
                        DbMessage.chat_id == chat_id,
                        DbMessage.deleted == False
                    )
                    # .options(joinedload(DbMessage.file))
                    .order_by(DbMessage.timestamp, DbMessage.message_id)
                )

                result = await session.execute(stmt)
                messages = result.scalars().unique().all()
                return messages
            except IntegrityError as e:
                raise ValueError(f"Ошибка целостности данных: {str(e)}")
            except DataError as e:
                raise ValueError(f"Ошибка типа данных: {str(e)}")
            except Exception as e:
                raise ValueError(f"Неожиданная ошибка: {str(e)}")

    @staticmethod
    async def mark_read(sender_id, chat_id):
        async with async_session_factory() as session:
            try:
                is_member = await session.scalar(
                    select(func.count())
                    .select_from(DbChatMember)
                    .where(
                        DbChatMember.chat_id == chat_id,
                        DbChatMember.user_id == sender_id
                    )
                )
                if not is_member:
                    raise PermissionError("Пользователь не является участником этого чата")
                read_at = datetime.now()
                stmt = update(DbMessage).where(DbMessage.chat_id == chat_id, DbMessage.receiver_id == sender_id).values(
                    read_at=read_at, read=True)
                result = await session.execute(stmt)
                marked_count = result.rowcount
                await session.commit()
                return {"marked_count": marked_count, "read_at": read_at}
            except IntegrityError as e:
                await session.rollback()
                raise ValueError(f"Ошибка целостности данных: {str(e)}")
            except DataError as e:
                await session.rollback()
                raise ValueError(f"Ошибка типа данных: {str(e)}")
            except Exception as e:
                await session.rollback()
                raise ValueError(f"Неожиданная ошибка: {str(e)}")

    @staticmethod
    async def get_user_by_username(username: str) -> dict:
        async with async_session_factory() as session:
            try:
                stmt = select(DbUser.user_id, DbUser.username, DbUser.hashed_password, DbUser.is_disabled, DbUser.avatar_file_id).where(DbUser.username == username)
                result = await session.execute(stmt)
                return result.mappings().first()
            except IntegrityError as e:
                raise ValueError(f"Ошибка целостности данных: {str(e)}")
            except DataError as e:
                raise ValueError(f"Ошибка типа данных: {str(e)}")
            except Exception as e:
                raise ValueError(f"Неожиданная ошибка: {str(e)}")

    @staticmethod
    async def upload_file(file: UploadFile, user_id):
        async with async_session_factory() as session:
            try:
                file_data = await file.read()
                db_model = DbFile(filename=file.filename, content_type=file.content_type,
                            data=file_data, user_id=user_id, uploaded_at=datetime.now())
                session.add(db_model)
                await session.commit()
                await session.refresh(db_model)
                return db_model

            except IntegrityError as e:
                await session.rollback()
                raise ValueError(f"Ошибка целостности данных: {str(e)}")
            except DataError as e:
                await session.rollback()
                raise ValueError(f"Ошибка типа данных: {str(e)}")
            except Exception as e:
                await session.rollback()
                raise ValueError(f"Неожиданная ошибка: {str(e)}")

    @staticmethod
    async def get_file_by_id(file_id):
        async with async_session_factory() as session:
            try:
                stmt = select(DbFile).where(DbFile.file_id == file_id)
                result = await session.execute(stmt)
                return result.scalar_one_or_none()

            except IntegrityError as e:
                raise ValueError(f"Ошибка целостности данных: {str(e)}")
            except DataError as e:
                raise ValueError(f"Ошибка типа данных: {str(e)}")
            except Exception as e:
                raise ValueError(f"Неожиданная ошибка: {str(e)}")

    @staticmethod
    async def get_user_by_user_id(user_id):
        async with async_session_factory() as session:
            try:
                stmt = select(DbUser.user_id, DbUser.username, DbUser.is_disabled, DbUser.avatar_file_id
                              ).where(DbUser.user_id == user_id)
                result = await session.execute(stmt)
                return result.mappings().first()

            except IntegrityError as e:
                raise ValueError(f"Ошибка целостности данных: {str(e)}")
            except DataError as e:
                raise ValueError(f"Ошибка типа данных: {str(e)}")
            except Exception as e:
                raise ValueError(f"Неожиданная ошибка: {str(e)}")

    @staticmethod
    async def create_direct_chat(sender_id: int, receiver_id: int) -> Chat:
        if sender_id == receiver_id:
            raise ValueError("Direct chat requires two different users")

        async with async_session_factory() as session:
            async with session.begin():
                users_rows = await session.execute(
                    select(DbUser.user_id).where(DbUser.user_id.in_([sender_id, receiver_id]))
                )
                users_found = {row[0] for row in users_rows}
                if len(users_found) != 2:
                    raise ValueError("One or both users do not exist")

                existing_chat_id = await session.scalar(
                    select(DbChat.chat_id)
                    .join(DbChatMember, DbChatMember.chat_id == DbChat.chat_id)
                    .where(
                        DbChat.is_group == False,
                        DbChatMember.user_id.in_([sender_id, receiver_id]),
                    )
                    .group_by(DbChat.chat_id)
                    .having(func.count(DbChatMember.user_id) == 2)
                    .limit(1)
                )
                if existing_chat_id:
                    existing_chat = await session.get(DbChat, existing_chat_id)
                    return existing_chat

                new_chat = DbChat(is_group=False, created_at=datetime.now())

                session.add(new_chat)
                await session.flush()
                chat_model = Chat.model_validate(new_chat)
                session.add_all([
                    DbChatMember(chat_id=new_chat.chat_id, user_id=sender_id),
                    DbChatMember(chat_id=new_chat.chat_id, user_id=receiver_id),
                ])

                await session.refresh(new_chat)
                return chat_model




async def create():
    await ORM.create_tables()

if __name__ == "__main__":
    asyncio.run(create())






