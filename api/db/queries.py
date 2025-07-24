import asyncio
from datetime import datetime
from fastapi import UploadFile, HTTPException
from sqlalchemy import select, or_, and_, update
from sqlalchemy.exc import IntegrityError, DataError
from sqlalchemy.orm import joinedload

from api.schemas.file_schema import MAX_FILE_SIZE
from api.schemas.message_schema import MessageInDb, Message
from api.db.connect import async_session_factory, Base, engine
from api.db.models import DbMessage, DbUser, DbFile
from api.schemas.user_schemas import User, UserInDb



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
    async def get_dialog(sender_id, receiver_id) -> list[MessageInDb]:
        async with async_session_factory() as session:
            try:
                where_condition = and_(
                    or_(
                        and_(DbMessage.sender_id == sender_id, DbMessage.receiver_id == receiver_id),
                        and_(DbMessage.sender_id == receiver_id, DbMessage.receiver_id == sender_id)
                    ),
                    DbMessage.deleted == False
                )

                stmt = (
                    select(DbMessage)
                    .where(where_condition)
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
    async def mark_read(sender_id, receiver_id):
        async with async_session_factory() as session:
            try:
                read_at = datetime.now()
                result = await session.execute(
                    update(DbMessage).where(
                        (
                            (DbMessage.sender_id == receiver_id) & (DbMessage.receiver_id == sender_id) & (DbMessage.read == False)
                        )
                    ).values(read= True, read_at=read_at)
                )
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
                stmt = select(DbUser.user_id, DbUser.username, DbUser.hashed_password, DbUser.is_disabled).where(DbUser.username == username)
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


async def create():
    await ORM.create_tables()

if __name__ == "__main__":
    asyncio.run(create())






