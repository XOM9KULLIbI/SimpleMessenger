from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine
from sqlalchemy.orm import DeclarativeBase
from api.settings import db_settings
import asyncpg


engine = create_async_engine(url=db_settings.db_url)
async_session_factory = async_sessionmaker(engine)

class Base(DeclarativeBase):
    pass